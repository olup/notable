<!-- markdownlint-configure-file {
  "MD013": {
    "code_blocks": false,
    "tables": false
  },
  "MD033": false,
  "MD041": false
} -->

<div align="center">

![Notable App](https://github.com/Ethran/notable/blob/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png?raw=true "Notable Logo")
# Notable (Ethran Fork)

<a href="https://github.com/Ethran/notable/releases/latest">
  <img src="https://img.shields.io/badge/-download%20here-informational" alt="Download here">
</a><br/>

<a href="https://github.com/Ethran/notable/releases/latest">
  <img src="https://img.shields.io/github/downloads/Ethran/notable/total?color=47c219" alt="Downloads">
</a>
<a href="https://discord.com/invite/X3tHWZHUQg">
  <img src="https://img.shields.io/badge/discord-7289da.svg" alt="Discord">
</a>

[Features](#features) •
[Download](#download) •
[Gestures](#gestures) •
[Supported Devices](#supported-devices) •
[Contribute](#contribute)

</div>


## About This Fork

This repository, maintained by **Ethran**, is a personal project based on the original [olup/notable](https://github.com/olup/notable) repository.  
The base repository has been inactive for over a year, and this fork introduces updates and features tailored to my specific needs and preferences.

### What's New in This Fork?
- Semi-active development with regular updates.
- Personal features and optimizations that make the app more functional for my use.
- Pre-release builds with experimental features and enhancements.

> ⚠️ Please note: Since this is a personal project, the features and updates reflect what I find useful. However, feedback and suggestions are welcome!

---

## Features
* ⚡ **Fast Page Turn with Caching:** Notable leverages caching techniques to ensure smooth and swift page transitions, allowing you to navigate through your notes seamlessly. (next and previous pages are cached)
* ↕️ **Infinite Vertical Scroll:** Enjoy a virtually endless canvas for your notes. Scroll vertically without limitations.
* 📝 **Quick Pages:** Quickly create a new page using the Quick Pages feature.
* 📒 **Notebooks:** Keep related notes together and easily switch between different noteboo︂︂ks based on your needs.
* 📁 **Folders:** Create folders to organize your notes.
* 🤏 **Editors' Mode Gestures:** [Intuitive gesture controls](#gestures) to enhance the editing experience.
* 🌅 **Images:** Add, move, scale, and remove images
* ︂︂᠋︁  **selection export** share selected text

## Download
**Download the latest stable version of the [Notable app here.](https://github.com/Ethran/notable/releases/latest)**

Alternatively, get the latest build from main from the ["next" release](https://github.com/Ethran/notable/releases/next)

Open up the '**Assets**' from the release, and select the `.apk` file.

<details><summary title="Click to show/hide details">❓ Where can I see alternative/older releases?</summary><br/>
You can go to original olup <a href="https://github.com/olup/notable/tags" target="_blank">'Releases'</a> and download alternative versions of the Notable app.
</details>

<details><summary title="Click to show/hide details">❓ What is a 'next' release?</summary><br/>
The 'next' release is a pre-release, and will contain features implemented but not yet released as part of a version - and sometimes experiments that could very well not be part a release.
</details>

## Gestures
Notable features intuitive gestures controls within Editor's Mode, to optimize the editing experience:
#### ☝️ 1 Finger
* **Swipe up or down**: Scroll the page.
* **Swipe left or right:** Change to the previous/next page (only available in notebooks).
* **Double tap:** Show or hide the toolbar.
* ~**Double tap bottom part of the screen:** Show quick navigation.~
* **Hold** select image
#### ✌️ 2 Fingers
* **Swipe left or right:** Undo/redo your changes.
* **Single tap:** Switch between writing modes and eraser modes.

#### 🔲 Selection
* **Drag:** Move the selected writing around.
* **Double tap:** Copy the selected writing.

## Supported Devices

The following table lists devices confirmed by users to be compatible with specific versions of Notable.  
This does not imply any commitment from the developers. Feel free to add your device to the list if tested successfully.

| Device Name                                                                           | v0.0.10 | v0.0.11dev |        |        |        |
|---------------------------------------------------------------------------------------|---------|------------|--------|--------|--------|
| [ONYX BOOX Go 10.3](https://onyxboox.com/boox_go103)                                  | ✔       | ?          |        |        |        |
| [Onyx Boox Note Air 4 C](https://onyxboox.pl/en/ebook-readers/onyx-boox-note-air-4-c) | ✘       | ✔          |        |        |        |
| [Onyx Boox Note Air 3 C](https://onyxboox.pl/en/ebook-readers/onyx-boox-note-air-3-c) | ✘       | ✔          |        |        |        |
| [Onyx Boox Note Max](https://shop.boox.com/products/notemax)                          | ✘       | ✔          |        |        |        |

Feel free to add your device if tested successfully!


## Screenshots

<div style="display: flex; flex-wrap: wrap; gap: 10px;">

<img src="https://github.com/user-attachments/assets/1dc04156-06f3-424c-92ee-9671c48fb83d" alt="screenshot-1" style="width:200px;"/>

<img src="https://github.com/user-attachments/assets/83895c63-7ffa-4558-8a5e-4742460d0e17" alt="screenshot-2" style="width:200px;"/>

<img src="https://github.com/user-attachments/assets/784c1954-d83b-4d43-8dfb-65478a8a1d9e" alt="screenshot-3" style="width:200px;"/>


<img src="https://github.com/user-attachments/assets/152265d5-b520-4d99-919c-754c8e6a7f8e" alt="screenshot-5" style="width:200px;"/>

<img src="https://github.com/user-attachments/assets/15a9f0a7-5326-4b5d-880c-a31b95a4d9bd" alt="screenshot-6" style="width:200px;"/>

<img src="https://github.com/user-attachments/assets/ac9f9138-948d-47d5-b94f-e721429f886f" alt="screenshot-7" style="width:200px;"/>

</div>



## Contribute
Notable is an open-source project and welcomes contributions from the community. 
To start working with the project, see [the guide on how to start contributing](https://docs.github.com/en/get-started/quickstart/contributing-to-projects) to the project. 

***Important:*** Be sure to edit the `DEBUG_STORE_FILE` variable in the `/app/gradle.properties` file to the keystore on your own device. This is likely stored in the `.android` directory on your device.

***Important:*** To use your BOOX device for debugging, an application will be required to enable developer mode on your BOOX device. [See a short guide here.](https://imgur.com/a/i1kb2UQ)  
