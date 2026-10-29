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

import net.akehurst.app.sysml.api.ModelRepository
import net.akehurst.app.sysml.api.ModelRepositoryFactory
import net.akehurst.app.sysml.info.ModelStorage

class InMemoryModelRepository(override val storage: ModelStorage) : ModelRepository {
    private val files = linkedMapOf<String, String>()

    override suspend fun open() {}
    override suspend fun listFiles(): List<String> = files.keys.sorted()
    override suspend fun read(path: String): String? = files[path]
    override suspend fun write(path: String, text: String, message: String) {
        files[path] = text
    }

    override suspend fun delete(path: String, message: String) {
        files.remove(path)
    }

    override suspend fun markBaseline(name: String): String? = null
}

/**
 * The storage available on the platform.
 * JVM: local file system and git. Web: in memory only (first cut, a server is needed for file system and git).
 */
expect fun platformModelRepositoryFactory(): ModelRepositoryFactory
