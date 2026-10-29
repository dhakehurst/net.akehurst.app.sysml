# Architecture (first cut)

The code is split by viewpoint, under `3_Code`.

| Folder | Module | Responsibility |
|---|---|---|
| information | `sysml-info` | Data: users, roles, projects, model storage, processes, agent strategy, templates, diagrams, baselines, and a light weight view of the SysML model. |
| computational | `user-api` | Service interfaces used by the GUI: authentication, authorisation, admin, projects, model repositories, process, AI assistant, diagrams, documents, baselines. |
| computational | `sysml-core` | Implementations of the services. |
| engineering | `sysml-gui` | Compose Multiplatform user interface. Depends only on `user-api` and `sysml-info`. |
| application | `sysml-app` | `SysmlApplication` wires `sysml-core` to `sysml-gui`, plus the JVM and wasmJs `main` functions. |

## Same code for JVM and web [REQ-1.2]

Everything is in `commonMain`, except:
- model storage: JVM has local file system and git (git command line); web keeps projects in memory, and needs a server for file system and git;
- the two `main` functions.

## Main parts of sysml-core

- `SysmlServices`: wires all services together; `Defaults` holds a starting process, project template, document template and development users.
- `security`: password and single sign on login, role based authorisation. SSO token checking is a development stub (`DevelopmentSsoTokenValidator`) until OpenID Connect is added.
- `project`: projects, process state, baselines. Everything is held in memory for now.
- `storage`: model repositories. A git baseline is an annotated tag `baseline/<name>`, pushed with the branch.
- `model`: `SysmlTextScanner`, a simple reader of SysML v2 text. To be replaced by an agl parser producing the omg sysml model.
- `diagram`: selects elements, lifts relationships of hidden features to their owner, lays out in layers, renders SVG.
- `document`: Markdown document templates with `{{...}}` placeholders.
- `ai`: `KoogAiAssistant`, a Koog agent (Anthropic model) whose prompts come from the process' agent strategy.
