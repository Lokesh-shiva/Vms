# Plixo Control — Release Keystore Setup

One-time setup. Do this before building a release APK/AAB.

## Step 1: Generate the keystore

Run this in your terminal (anywhere — you'll move the file in step 2):

```bash
keytool -genkey -v \
  -keystore plixo-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias plixo-key
```

You'll be prompted for:
- Keystore password (pick a strong one, save it somewhere safe)
- Key password (can be the same as keystore password)
- Your name, organisation, city, country

## Step 2: Move the keystore file

Place `plixo-release.jks` in the `Vmsadminapp/` directory (next to `app/`).
DO NOT commit it to git — add `*.jks` to .gitignore if not already there.

## Step 3: Fill in local.properties

Copy `local.properties.template` to `local.properties` and fill in:

```
KEYSTORE_PATH=../plixo-release.jks
KEYSTORE_PASSWORD=<your keystore password>
KEY_ALIAS=plixo-key
KEY_PASSWORD=<your key password>
```

## Step 4: Build a signed release

In Android Studio:
Build → Generate Signed Bundle / APK → Android App Bundle → select release config → finish.

Or via command line:
```
gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## ⚠️ IMPORTANT

- Back up `plixo-release.jks` somewhere safe (cloud, USB, password manager)
- If you lose this file you can NEVER update your Play Store app
- Never commit it to git or share it publicly
