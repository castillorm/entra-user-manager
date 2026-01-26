# Entra User Manager

A Java-based utility for managing users in **Microsoft Entra ID (Azure Active Directory)** using the **Microsoft Graph REST API**.

This project is designed for **restricted / offline environments**:
- ❌ No Maven / Gradle
- ❌ No external dependency repositories
- ✅ Uses only pre-approved `.jar` files
- ✅ Built with **Apache Ant**
- ✅ Configured via `.ini` file
- ✅ Works cleanly in Eclipse

---

## Features

- Authenticate to Microsoft Graph using **OAuth2 client credentials**
- List users
- Get user details
- Enable / disable user accounts
- Invite **B2B guest users**
- Fully offline dependency model (`libs/*.jar`)
- Ant-based build and run
- Eclipse-friendly project layout

---

## Project Structure

```

entra-user-manager/
├── build.xml
├── build.properties
├── README.md
├── .gitignore
├── config/
│   └── entra.ini.template
├── libs/
│   └── README.md
└── src/
└── main/
└── java/
└── com/yourorg/entra/
├── Main.java
├── IniConfig.java
├── GraphTokenProvider.java
├── GraphHttpClient.java
└── EntraUserManager.java

````

---

## Dependencies (Offline)

All dependencies must be placed manually in the `libs/` directory.  
They are **not committed** to this repository.

Required jars:

- Apache HttpClient 4.5.x  
- Apache HttpCore 4.4.x  
- Commons Configuration 1.10  
- Commons Lang 2.6  
- Commons Logging  
- Commons Codec  
- Jackson Core / Databind / Annotations 2.13.x  

See `libs/README.md` for details.

---

## Configuration

Configuration is loaded from an `.ini` file.

### Template

`config/entra.ini.template` (committed)

```ini
[graph]
tenantId=YOUR_TENANT_ID
clientId=YOUR_CLIENT_ID
clientSecret=YOUR_CLIENT_SECRET
authorityHost=https://login.microsoftonline.com
graphBaseUrl=https://graph.microsoft.com
scope=https://graph.microsoft.com/.default

[app]
timeoutSeconds=30
````

### Runtime file

Copy the template to:

```
config/entra.ini
```

⚠️ **Never commit real credentials** — `.ini` files are gitignored.

---

## Required Entra ID Permissions

This application uses **application (app-only) permissions**.

Grant and admin-consent the following in your App Registration:

* `User.Read.All`
* `User.ReadWrite.All`
* `User.Invite.All` (for B2B guests)

---

## Building with Ant

### Compile

```sh
ant compile
```

### Run

```sh
ant run -Dini.path=config/entra.ini
```

### Build runnable JAR (no dependencies bundled)

```sh
ant jar
```

### Run JAR

```sh
ant run-jar -Dini.path=config/entra.ini
```

---

## Eclipse Setup

### Source Folder

Mark this folder as the source root:

```
src/main/java
```

Right-click → **Build Path → Use as Source Folder**

### Output Folder

Set Eclipse’s output folder to match Ant:

```
build/classes
```

(Project → Properties → Java Build Path → Source tab)

### Recommendation

* Disable *Build Automatically*
* Let Ant be the source of truth

---

## B2B Guest Invitations

Guest users are created using:

```
POST /v1.0/invitations
```

Implemented via:

```java
EntraUserManager.inviteGuest(...)
```

Supports:

* Sending invitation emails
* Custom message body
* Custom redirect URL
* App-only permissions

---

## Security Notes

* No credentials are stored in source code
* `.ini` files are excluded via `.gitignore`
* Dependency jars are never committed
* Designed for controlled enterprise environments

---

## License

Choose a license appropriate for your organization (MIT recommended for samples).

---

## Disclaimer

This project is not an official Microsoft product and is provided as-is.
