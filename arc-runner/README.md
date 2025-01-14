<!--
SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others

SPDX-License-Identifier: CC0-1.0    
-->
# Arc Runner (Experimental)

## Install Arc-Runner

1. Install Arc Runner

On Linux, macOS, and Windows (using WSL or bash compatible shell like Cygwin or MinGW)
```
curl -Ls https://sh.jbang.dev | bash -s - app install --fresh --force https://github.com/eclipse-lmos/arc/blob/main/arc-runner/arc.java
```

On Windows using Powershell:
```
Set-ExecutionPolicy RemoteSigned -scope CurrentUser
iex "& { $(iwr https://ps.jbang.dev) } trust add https://github.com/eclipse-lmos/arc/blob/main/arc-runner/"
iex "& { $(iwr https://ps.jbang.dev) } app install --fresh --force https://github.com/eclipse-lmos/arc/blob/main/arc-runner/arc.java"
```
(This will install everything you need to run the arc-runner, including JAVA)

2. Open a new console window and install the Weather Agent
```
arc install weather
```
(This will install the weather agent located at https://github.com/eclipse-lmos/arc/blob/main/arc-runner/weather.agent.kts)

3. Set environment variables

On Linux, macOS, and Windows (using WSL or bash compatible shell like Cygwin or MinGW)
```
export ARC_AI_URL=https://gpt4-se-dev.openai.azure.com/ // The url hosting the models. Can be omitted if using openai.
export ARC_AI_KEY=YOUR_OPENAI_KEY // The key to authenticate with the AI service. can be omitted if using Azure Login.
export ARC_CLIENT=openai // or azure, ollama, openai-sdk etc.
export ARC_MODEL=gpt-4o // the name of the model to use
```

On Windows using Powershell:
```
$env:ARC_AI_URL="https://gpt4-se-dev.openai.azure.com/" // The url hosting the models. Can be omitted if using openai.
$env:ARC_AI_KEY="YOUR_OPENAI_KEY" // The key to authenticate with the AI service. can be omitted if using Azure Login.
$env:ARC_CLIENT="openai" // or azure, ollama, openai-sdk etc.
$env:ARC_MODEL="gpt-4o" // the name of the model to use
```

4. Run Arc

```
arc run
```


4. Chat with the weather agent
```
open browser at http://localhost:8080/chat/index.html
```
