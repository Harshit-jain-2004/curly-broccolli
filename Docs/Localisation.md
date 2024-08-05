# Localisation

Localisations are carried out via Touchdown service, [configuration](https://tdb-dashboard-prod.azurewebsites.net/Home/ConfigSettings?TeamID=377).

In the configuration, Extent Of Localisation (EOL) currently list all 68 languages SDK supports. The list of languages is the same for web and iOS SDKs.

Touchdown takes a few days to finalise translations after new strings are added. **Allocate enough time when adding a new string to the project.** ~5 days before release is a good guideline to aim for, but the earlier the better.

Before translated strings are available, strings will still be generated for all languages, but in fallback language - English.

The localisation is currently done through this [pipeline](https://notessdk.visualstudio.com/Sticky%20Notes%20SDK/_build?definitionId=4). The strings are sent to the translation service every day from develop branch via a scheduled build.

Currently, strings need to be committed to the repository manually. This could be automated in the future. Some manual checking to ensure whether all translations for all languages were received might still be needed in that case.