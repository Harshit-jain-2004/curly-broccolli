# Building and Managing Releases

## Getting Started

In order to ship the Android SDK, you'll need to do the following:
* Read a tutorial on Git Flow, such as this one: https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow
* Make sure your locally installed version of Git is up to date.
* Run 'git flow init' from your notes-android-sdk repo, choosing the default for all options, except for the Version Tag Prefix, which should just be 'v' (without quotes). Note that these settings are stored locally and you'll need to run this command once for each new repo.

## How Versioning Works

Our SDK follows [Semantic Versioning](https://semver.org/), which defines specific semantics that we must follow to help our partners consume our SDK and be aware of API changes. A version number looks like: A.B.C\[-beta.D\]

**A** is the major version number. We increment it anytime we make any breaking change to our public API, usually because we're adding significant new functionality. We try not to do this more than once every few months, because it means that our partners may be forced to make changes before they can consume the new SDK. If we need to change a public API, but we can support both the old and new versions of the API simultaneously, we should do this and mark the old API as deprecated. This gives our partners a chance to move to the new API before the old one is removed, and helps them stay on the latest version of the SDK.

**B** is the minor version number. We increment it anytime we make non-breaking changes to our public API, usually because we're adding a small amount of new functionality, or because a bug fix required an API change. Partners should be more easily able to upgrade across minor versions because they should not cause compilation failures, but they may want to do work on their side to support new features, or may need to do additional testing on the new functionality.

**C** is the patch version number. We increment it anytime we are releasing a version that contains only bug fixes, with no changes to the public API or new functionality. Partners should be able to upgrade across patch versions with high confidence in stability, minimal testing and no code changes required on their side.

**D** is the optional beta version number. We release betas when we want to co-develop work with our partners, or release in-development work to select partners' dogfood or other internal rings. We provide no guarantees that APIs or functionality will not change drastically between beta versions and the release version, and do not recommend our partners consume beta versions unless they are working directly with us. Beta versions should never be shipped to production customers because they have not been fully validated.

## Types of Release Branches

### Hotfix Branches

Hotfixes are quick fixes made on the latest release version in master, usually because a partner reported a bug or we found it ourselves, and we can't or don't want to wait until the next full release to get the fix out. Typically a hotfix will bump the patch version, which will enable our partners to get the fix out to their customers with the least amount of work. In exceptional cases we may need to bump the minor version because we're forced to add a new API to fix a bug, or we do more substantial work in a hotfix because the develop branch is too unstable to be ready to release soon.

Here's how to make a hotfix:

1. Check to see if a hotfix is already in progress - if one is and we want to include your fix in it, then you can skip this step. Otherwise, you can start a new hotfix branch with `git flow hotfix start <version>` and push it to origin. (Don't include the "v" in the version here - that's only necessary when creating tags manually, such as when tagging beta versions.) You should be able to intuit what the version number should be based on the above and the change you plan to make. But don't be afraid to check with other team members if you're unsure.
2. Start a new branch based on the hotfix branch you created (or that already existed) in step 1. Make your changes in this branch and be sure to test thoroughly, since hotfix branches don't get as much dev usage as our development branch. When you're ready, push your branch and open a PR, being sure to choose the **hotfix branch** as the merge target rather than develop. If you already did your work elsewhere, such as if you merged your change into develop but later decided you want to backport it to a hotfix, then you can cherry-pick your merge commit with `git cherry-pick <commit-id> -m 1`.
3. Once your PR is signed off and merged into the hotfix branch, coordinate with the team to see if anyone else had fixes they wanted to get into the same hotfix, or if it's ready to be released. If multiple PRs are being merged into a single hotfix, it's a good idea for someone to checkout the hotfix branch after everything's been merged, build it and do some final validation locally.
4. To release the hotfix, make sure you're fully fetched/pulled and run `git flow hotfix finish -p <version>`. Some vi editors will pop up - you can leave the merge commit messages as the default, but add some light release notes for the tag. Depending on what's changed, you may need to address merge conflicts as well. Once the command has completed, the hotfix branch will be merged into both develop and master, and the release will be tagged and kicked off on the build machine.

### Release Branches

Whereas hotfixes are small fixes made on master, we use release branches when we want to release all of the work that we've done in develop since the last release. These will usually bump the major or minor version, depending on the scope of the changes. There is no hard and fast rule for when we create a release branch - we may go longer in between when working on big new features, and may release more frequently when we are making smaller, more incremental changes. This is in part to ensure that we can rev our APIs while they are under development without creating unnecessary work for our partners.

Deciding when it's time to create a release branch off of develop is a shared team decision, based on factors like our team goals, feature completeness, build quality, and API stability. The benefit of a release branch is that it gives us the ability to finalize our release with necessary bug fixes while simultaneously unblocking potentially destabilizing work which can be checked into develop to be included in the next release. Exactly how long a release branch should live is also a judgment call, depending on the scope of the release and how much validation we want from internal rings before we feel that our code is production ready. If a release is fairly small and we're confident in its stability, the release branch may be short-lived, or even immediately finished and merged into master, since we still have the option to ship hotfixes as needed.

Making a release is similar to a hotfix:

 1. Determine the version number, create the release branch with `git flow release start <version>` and push it to origin.
 2. If we're looking to get internal dogfood usage and telemetry before we ship, now might be a good time to release a beta version (see below).
 3. When you want to get a fix into the release, base your branch on the release branch and set the release branch as the merge target for your PR, just like with hotfix branches. If you don't want your changes to be included in the release, work off of develop as normal.
 4. When we're ready to be done with the release, run `git flow release finish -p <version>` which, like with hotfixes, will merge into master and develop, prompting you a few times during the process,  and the release will be tagged and kicked off on the build machine.

### Support Branches

We can use support branches if we ever need to patch an older version that's not the latest version in master. Since this is hopefully a very rare occurrence, they are only created on demand. As an example, let's say a partner is currently using v17.2.3 in production, and a critical bug (such as a security or data loss issue) is discovered in this build. We want to issue a hotfix so we can get the fix out to production as quickly as possible, but master has already moved onto v18+ because the partner is a bit behind, and it's not acceptable for the partner to upgrade all the way to v18 because this would introduce too much risk. In this case, we would create a support branch so we can release a hotfix for v17.

Support branches are permanent forks off of master, started from the last release from the version we want to support. They function like master, in the sense that we can create a hotfix branch from the support branch, merge any fixes into the hotfix branch, and then merge the hotfix branch back into the support branch to finish the hotfix. They never merge back into master or develop, so any work will need to be done in multiple different branches in Git to ensure the fix ships in all of the places it needs to.

Ideally we should never need to do this, and the best way we can avoid this is to help our partners stay on the latest version as much as possible by limiting breaking changes. But if it does come up, we have a mechanism to handle it, and we should work together and read the Git Flow documentation carefully to ensure that we're doing everything correctly. Instructions are not included here since it's not a process we've ever gone through.

### Beta Releases

Beta releases are not part of the Git Flow branching structure, and we can release a beta version from any branch, including develop, hotfix branches, and release branches, depending on our needs. Whereas stable releases are tagged automatically by git flow commands, beta releases need to be tagged manually. Simply tag the commit you wish to release as a beta version on GitHub or from your local git repo and push the tag. The version number should be the same as what it will be when are ready to release a stable version, so if for instance we're currently working on v19.0.0 in develop, we would tag the first beta as `v19.0.0-beta.1`. The number after `beta.` is incremented for each subsequent beta release.

## Release Pipeline

The same [release pipeline](https://office.visualstudio.com/OneNote/_build?definitionId=5201) is used for all releases, hotfixes, and betas. It's kicked off automatically when a tag starting with "v" is created or pushed to GitHub - either as part of a Git Flow command or manually. The pipeline performs the following steps:
1. Automatically set the version in the [artifact.properties](../artifact.properties) file based on the tag. This version can be referenced in code as `BuildConfig.VERSION_NAME` and will be used as the version number for the Maven and NuGet packages. (Note: This version number is checked in as "dev" so we can distinguish between official releases and local dev builds.)
2. Build the release binaries.
3. Pack the AAR and POM files into a NuGet package, using the checked in version of [StickyNotes.Android.nuspec](../StickyNotes.Android.nuspec) file, which follows the schema described here: https://microsoft.sharepoint-df.com/teams/remember/_layouts/OneNote.aspx?id=%2Fteams%2Fremember%2FShared%20Documents%2FAll%20Microsoft%2FNotebooks%2FSticky%20Notes&wd=target%28%F0%9F%9B%B9%20Team%20Bravo%2FGetting%20Started.one%7C916B30A5-B5D3-4EDB-8AD8-449A66E4636B%2FAndroid%20SDK%20Nuget%20creation%20and%20consumption%7CCA606C6F-711D-478E-BE07-3C4195565096%2F%29
4. Publish the release to our Maven feed here: https://office.visualstudio.com/OneNote/_packaging?_a=package&feed=stickynotes-android-sdk&view=overview&package=com.microsoft.stickynotes%3Aandroid&protocolType=maven
5. Publish the release to the Office NuGet feed here: https://office.visualstudio.com/OE/_packaging?_a=package&feed=Office&view=overview&package=StickyNotes.Android&protocolType=NuGet

Once the release is done, you can send an email to NotesSDKPartners@microsoft.com mailing list, detailing the new changes.

### Publishing Artifacts

To be able to publish the binary to the artifact repository, we need two environment variables in our build/release pipeline:

[Build pipeline](https://office.visualstudio.com/OneNote/_apps/hub/ms.vss-ciworkflow.build-ci-hub?_a=edit-build-definition&id=5201&view=Tab_Variables)

- `AZURE_ARTIFACTS_ENV_ACCESS_TOKEN`: this is the **access token* that grants access to our artifact repository for both publish and consume artifacts.
If you need to create a token you can do it [here](https://office.visualstudio.com/_usersSettings/tokens) under `office` account and using the `Packaging (Read & write)` permission.
- `REMOTE_OUTPUT`: this is the path where the generated release SDK binary is located. It's used by the publish task to create the artifact. 
As an example: `noteslib/build/outputs/aar/noteslib-release.aar` is the current path where we have the generated SDK release binary.

If you want to change the build/release pipelines, **don't forget to add these two variables** (and their values) to your pipeline's environment variables.
