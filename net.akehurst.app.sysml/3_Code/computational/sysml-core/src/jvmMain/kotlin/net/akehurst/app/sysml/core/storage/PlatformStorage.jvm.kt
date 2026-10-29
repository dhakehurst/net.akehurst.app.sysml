/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.app.sysml.core.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.akehurst.app.sysml.api.ModelRepository
import net.akehurst.app.sysml.api.ModelRepositoryFactory
import net.akehurst.app.sysml.info.ModelStorage
import java.io.File

actual fun platformModelRepositoryFactory(): ModelRepositoryFactory = ModelRepositoryFactory { storage ->
    when (storage) {
        is ModelStorage.LocalFileSystem -> LocalFileSystemModelRepository(storage, File(storage.rootPath))
        is ModelStorage.GitRepository -> GitModelRepository(storage)
        is ModelStorage.InMemory -> InMemoryModelRepository(storage)
    }
}

/** SysML v2 files (*.sysml) below a folder. [REQ-6.2.1] */
open class LocalFileSystemModelRepository(
    override val storage: ModelStorage,
    val root: File
) : ModelRepository {

    override suspend fun open() {
        withContext(Dispatchers.IO) { root.mkdirs() }
    }

    override suspend fun listFiles(): List<String> = withContext(Dispatchers.IO) {
        root.walkTopDown()
            .onEnter { it.name != ".git" }
            .filter { it.isFile && it.extension == "sysml" }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .sorted()
            .toList()
    }

    override suspend fun read(path: String): String? = withContext(Dispatchers.IO) {
        resolve(path).takeIf { it.isFile }?.readText()
    }

    override suspend fun write(path: String, text: String, message: String) {
        withContext(Dispatchers.IO) {
            val f = resolve(path)
            f.parentFile?.mkdirs()
            f.writeText(text)
        }
    }

    override suspend fun delete(path: String, message: String) {
        withContext(Dispatchers.IO) { resolve(path).delete() }
    }

    override suspend fun markBaseline(name: String): String? = null

    /** a path inside [root]; paths that leave the root are refused */
    protected fun resolve(path: String): File {
        val f = File(root, path).canonicalFile
        require(f.toPath().startsWith(root.canonicalFile.toPath())) { "Path '$path' is outside the project folder" }
        return f
    }
}

/**
 * SysML v2 files in a git repository, using the `git` command line. [REQ-6.2.2]
 * Each write is committed locally. Setting a baseline creates an annotated tag and pushes the commits and the tag.
 */
class GitModelRepository(
    override val storage: ModelStorage.GitRepository
) : LocalFileSystemModelRepository(storage, File(storage.localPath)) {

    override suspend fun open() {
        withContext(Dispatchers.IO) {
            if (File(root, ".git").isDirectory) {
                git("pull", "--ff-only")
            } else {
                root.parentFile?.mkdirs()
                runGit(root.parentFile ?: File("."), "clone", "--branch", storage.branch, storage.url, root.absolutePath)
            }
        }
    }

    override suspend fun write(path: String, text: String, message: String) {
        super.write(path, text, message)
        withContext(Dispatchers.IO) {
            git("add", "--", path)
            if (hasStagedChanges()) git("commit", "-m", message)
        }
    }

    override suspend fun delete(path: String, message: String) {
        withContext(Dispatchers.IO) {
            git("rm", "--quiet", "--", path)
            git("commit", "-m", message)
        }
    }

    override suspend fun markBaseline(name: String): String = withContext(Dispatchers.IO) {
        val tag = "baseline/" + name.trim().replace(Regex("[^A-Za-z0-9._-]+"), "-")
        git("tag", "-a", tag, "-m", "Baseline $name")
        git("push", "origin", storage.branch)
        git("push", "origin", tag)
        tag
    }

    private fun hasStagedChanges(): Boolean =
        ProcessBuilder("git", "diff", "--cached", "--quiet").directory(root).start().waitFor() != 0

    private fun git(vararg args: String): String = runGit(root, *args)

    private fun runGit(dir: File, vararg args: String): String {
        val p = ProcessBuilder(listOf("git") + args).directory(dir).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        val code = p.waitFor()
        check(code == 0) { "git ${args.joinToString(" ")} failed ($code): $out" }
        return out
    }
}
