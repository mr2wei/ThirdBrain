## Changelog

All notable changes to this project are documented in this release.

## [4.0.0] - 2026-02-25

### 🚀 Features
- Added memory fragments for NPCs.
- Added per-NPC conversation range controls.
- Split zone behavior editing into a dedicated NPC zone config screen.
- Added new NPC commands for memory management and unlock flow.
- Improved NPC context/history handling for more consistent behavior.
- Added skin selector within npc creation and edit gui
- Added OpenAI TTS and carousel for selecting TTS voice
- Updated instructions to focus less on player companionship, rather in world NPCs

### 🛠 Bug Fixes / Improvements
- Improved OpenAI/Ollama client handling and runtime reliability.
- Improved NPC config serialization/loading behavior.
- Updated config/network handling to better support runtime updates.
- Refined UI flow for NPC/base config screens.
- Many QoL improvements such as:
    - Persistent entries per NPC
    - Global API Key input and Ollama/OpenAI endpoint URL
    - Maybe more, I forgor 💀

### 📚 Documentation
- Updated README to reflect ThirdBrain positioning and workflow.
- Added notes on tested providers and current testing scope.

### ⚙️ Misc
- Added `com.openai:openai-java:4.22.0`.
- Bumped version from `3.1.5` to `4.0.0`.

### ⚠️ Warning
- I did not test if existing NPCs will transfer gracefully
- At best, you only need to re enter Ollama URL, OpenAI URL, OpenAI API key
- At worst, everything is reset
