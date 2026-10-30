# writes the agl grammar into a Kotlin source file, so it is available on all platforms
import sys
src, dst = sys.argv[1], sys.argv[2]
text = open(src, encoding='utf-8').read().replace('$', "${'$'}")
q = '"""'
out = f'''/**
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

package net.akehurst.app.sysml.core.language

// GENERATED from sysml-core/grammar/SysMLv2_0.agl by grammar/tools/to_kotlin.py, do not edit.

/** The agl grammar of the SysML v2.0 textual notation. */
object SysmlGrammar {{
    const val GOAL_RULE = "RootNamespace"

    val text: String = {q}
{text}{q}
}}
'''
open(dst, 'w', encoding='utf-8').write(out)
