# Build agents. Configuration

We use `VSTS build agents` for:
- **Building our project** every time we push code to github.
- **Generating release binaries** with translated resources that are the ones we are going to share with partners.

These agents are daemons/services that run on physical machines, but that are handled by VSTS.
We create and initialize build agents in our machine that are registered into the service pool that you define in VSTS, so every time a push happens (or we trigger a build manually) an agent is chosen from the agent pool, and it's used to run a build.

We currently have two different domains and agent pools:
- Build our project: https://office.visualstudio.com/OneNote/_settings/agentqueues?queueId=47&_a=agents
- Build our release binaries: https://notessdk.visualstudio.com/Sticky%20Notes%20SDK/_settings/agentqueues?queueId=1&_a=agents

If we want to create a new agent and register it in one of the agent pools we own, we have to go to each agent pool website and click on the blue button **Download agent**.
There you will see the instructions to download, and set up a new agent.

You probably have to be in **Administrator mode/user** when setting up/deploying/stopping an agent.

Before to start the process, be sure that the environmental variables the agent is going to use are correctly set, for Android are:
- ANDROID_HOME (should the path to where your android sdk is)
- ANDROID_SDK (same that above)
- java (where your java sdk lives)
- JAVA_HOME (same that above)	
- jdk (same that above)

If you have a problem later when the agent is deployed and where the variables were wrongly set, see `Problems/how to` section later.

When creating a new agent, you have to follow some interactive steps. You will have to provide some info:

**- Domain:**

a) _Build agent_ domain: https://office.visualstudio.com/
b) _Release agent_ domain: https://notessdk.visualstudio.com/

**- Token:**

To be able to configure and deploy an agent, you have to provide as well a VSTS security token, so your agent can interact with VSTS, the agent pool, etc.

To access your tokens, you have to go to your profile (top right corner) and click on `security`. 
There you will be able to see the different tokens you have.

If you need to create a new token, you have to give the token **Agent pools (read & write)** permission. 
These are the only permissions required for our agents.

Once that you have your token, you just copy and paste it whenever the agent setup process asks for it.

If you already have a token created for your agent, and you are simply stopping and starting again your agent for maintenance tasks, go to your token, and click on `revoke` this will generate a new token, but using the same options you already had and giving you the opportunity to copy and paste the new token.
Don't worry, previous agents that were using an old token will continue working, and when you want to configure again your agent, you just can simply use the new token, it will work.

**- Agent pool:**

You will be asked for the name of the agent pool your new agent will be deployed.
a) _Build agent_: **Sticky Notes**
b) _Release agent_: **Default** (just tap on entering when asked).

**- Running as a daemon/service:**

You will be asked if you want to run your agent as a service/daemon.
I encourage you to say **yes** here so, the agent will run every time the machine is restarted without your intervention.


I think that's basically everything to set up a build agent.

### RunAndroidTests script

We have [this file](RunAndroidTests.yaml) that is a `yaml` script that can be imported as a task for the build pipeline that allows to run UI tests on docker containers.


# Problems/How to

**a)** Sometimes you want to restart an agent because maybe you did setup wrong your environmental variables.

You can do that by (being in your agent folder):
- Stopping your agent: `.\config.cmd remove`
- Running your agent again: `.\config.cmd`

You probably have to be in** Administrator mode/user**.

**b)** When updating an Android version you could enter in problems with the licenses approval. 

To make sure licenses are already approved you can do:
- Go to your android folder where `sdkmanager` executable lives.
- Type `.\sdkmanager.bat --licenses`

**c)** It can happen that when you build your project locally, the build is successfully done, but when the agent runs the build, it fails to be unable to download new packages.

Afaik, this happens because the local build and the agent build to run on different users/permissions, and the agents don't have enough privileges to make changes into your **android** folder, that is where the Android SDK and its lools live.

To fix that, what you can do is to give write or full access privileges to the agent service and/or other users you want to give access to the folder, so the agent is able to run and install new components. Of course, you should be the owner of the folder and/or have permissions to manage that folder.

If you face this problem: _"Permission error - Failed to enumerate objects in the container. Access is denied"_ when changing permissions to users, please check and follow the instructions written here: https://answers.microsoft.com/en-us/windows/forum/windows_8-security/permission-error-failed-to-enumerate-objects-in/93ea883f-853f-4981-a697-928bfbc71642?auth=1 This will fix the problem and then yowillll be able to change permissions in that folder.

# More info

You can read the official documentation here: https://docs.microsoft.com/en-us/azure/devops/pipelines/agents/agents?view=azdevops
