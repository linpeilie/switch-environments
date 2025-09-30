# switch-environments

![Build](https://github.com/linpeilie/switch-environments/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/28559-switchenvironments.svg)](https://plugins.jetbrains.com/plugin/28559-switchenvironments)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/28559-switchenvironments.svg)](https://plugins.jetbrains.com/plugin/28559-switchenvironments)

<!-- Plugin description -->

**A powerful IntelliJ IDEA plugin for managing environment variables with group support and beautiful UI.**

## Features

- 🗂️ **Group Management**: Organize environment variables into logical groups
- ✅ **Group Activation Control**: Enable/disable entire groups
- 📥 **Multi-format Import**: Import variables from .env, .properties, and .txt files
- 🎨 **Clean UI**: Modern list and table interface
- 💾 **Persistent Storage**: All settings are saved automatically
- 🔧 **Easy Management**: Add, edit, and delete variables with simple dialogs
- 🌍 **Global View**: Special first group shows all active variables

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "SwitchEnvironments"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/28559-switchenvironments) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/28559-switchenvironments/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/linpeilie/switch-environments/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Usage

### Accessing the Plugin

- Tool Window: View → Tool Windows → Environment Variables
- Menu: Tools → Environment Variables → Open Environment Manager

### Managing Groups

1. **Add Group**: Click the "+" button in the group toolbar
2. **Edit Group**: Select a group and click the edit button
3. **Delete Group**: Select a group and click the "-" button
4. **Activate/Deactivate**: Use checkboxes in settings or edit group dialog
5. **Import/Export**: Click the "⚙️" button in the group toolbar
6. **Sort**: Long-press and drag with the left mouse button to move it

### Built-in Groups

- **Env**：Read-only view of all active variables from enabled groups

### Managing Variables

1. **Add Variable**: Select a group (not built-in groups), then click "+" in the variables toolbar
2. **Edit Variable**: Double-click a variable or select and click edit button
3. **Delete Variable**: Select a variable and click "-" button
4. Variables are automatically active when their group is active
5. **Import**: Select a group (not built-in groups), then click "Import File" button

### Import Support

#### Import Config

The plugin supports exporting all data as well as importing that data.

#### Import Variables

The plugin supports multiple file formats:

- `.env` **files**: Standard environment file format
- `.properties` **files**: Java properties format with escape sequence support
- `.txt` **files**: Simple key=value format

##### Supported File Formats

- **.env files**:

```env
# This is a comment
DATABASE_URL=postgresql://localhost:5432/mydb
API_KEY=your_secret_key_here
DEBUG=true
APP_NAME="My Application"
```

- **.properties files**:

```properties
# Properties format
database.url=postgresql://localhost:5432/mydb
api.key=your_secret_key_here
debug=true
app.name=My Application
```

- **.txt files**:

```text
DATABASE_URL=postgresql://localhost:5432/mydb
API_KEY# Environment Variables Manager Plugin

A powerful IntelliJ IDEA plugin for managing environment variables with group support and beautiful UI.

## Features

- 🗂️ **Group Management**: Organize environment variables into logical groups
- ✅ **Activation Control**: Enable/disable entire groups or individual variables
- 📥 **Import Support**: Import variables from `.env` files
- 📤 **Export Support**: Export variables to `.env` files
- 🎨 **Beautiful UI**: Modern and intuitive user interface
- 💾 **Persistent Storage**: All settings are saved automatically
- 🔧 **Easy Management**: Add, edit, and delete variables with simple dialogs

## Installation

1. Clone this repository
2. Open the project in IntelliJ IDEA
3. Run `./gradlew buildPlugin` to build the plugin
4. Install the generated plugin file from `build/distributions/`

Or build and run directly:
```bash
./gradlew runIde
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

[Apache 2.0 License](https://raw.githubusercontent.com/linpeilie/switch-environments/master/LICENSE)

## Donation

If you like this plugin, you can [buy me a cup of coffee](https://afdian.com/a/linpeilie). Thank you!