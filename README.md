# Material Files (Enhanced Fork)

[![Android CI status](https://github.com/eetufin92/MaterialFiles/workflows/Android%20CI/badge.svg)](https://github.com/eetufin92/MaterialFiles/actions) [![License](https://img.shields.io/github/license/zhanghai/MaterialFiles?color=blue)](LICENSE)

An open source Material Design file manager for Android, enhanced with privacy and reliability improvements.

**Note:** This is a fork of the [original Material Files](https://github.com/zhanghai/MaterialFiles) by Hai Zhang. While it maintains the spirit and core of the original project, it includes several custom enhancements and is not the official repository.

## Key Enhancements in this Fork

- **Privacy First**: Removed non-free components, including Firebase Crashlytics and Google Services dependencies/initializers.
- **Improved SMB Reliability**:
  - Automatic retry for recoverable SMB errors (session expiration, broken pipes, timeouts).
  - Manual "Reset connection" and "Retry" buttons for SMB and file listing failures.
  - Fixed SMB hidden file detection using filesystem attributes.
- **Advanced Search Filters**:
  - Date filtering (e.g., `date:>2023-01-01`)
  - Size filtering (e.g., `size:<10MB`)
  - Negation filtering (e.g., `not:text`)
- **Better MIME Detection**:
  - Uses file size to distinguish between MPEG-TS video and TypeScript files (`.ts` files > 500KB are treated as video).
  - Fixed image opening priority to ensure internal viewers are preferred over generic "Save as" handlers.
- **Custom File Associations**:
  - Internal "Always open with" support for file extensions.
  - Dedicated "File associations" management UI in Settings to view and remove default apps.
- **UI Refinements**:
  - Refactored File List UI with better state persistence in search.
  - Updated progress and error layouts for a more robust experience.
- **Modernized Tooling**:
  - Upgraded to Gradle 9.4.1 and AGP 9.2.1.
  - Integrated Foojay toolchain resolver.
  - Updated to optimized `dav4jvm` fork.

## Features

- **Open source**: Lightweight, clean and secure.
- **Material Design**: Follows Material Design guidelines, with attention into details.
- **Breadcrumbs**: Navigate in the filesystem with ease.
- **Root support**: View and manage files with root access.
- **Archive support**: View, extract and create common compressed files.
- **NAS support**: View and manage files on FTP, SFTP, SMB and WebDAV servers.
- **Themes**: Customizable UI colors, plus night mode with optional true black.
- **Linux-aware**: Like [Nautilus](https://apps.gnome.org/Nautilus/), knows symbolic links, file permissions and SELinux context.
- **Robust**: Uses Linux system calls under the hood, not yet another [`ls` parser](https://news.ycombinator.com/item?id=7994720).
- **Well-implemented**: Built upon the right things, including [Java NIO2 File API](https://docs.oracle.com/javase/8/docs/api/java/nio/file/package-summary.html) and [LiveData](https://developer.android.com/topic/libraries/architecture/livedata).

## Preview

<p><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="32%" /> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="32%" /> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="32%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="32%" /> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="32%" /> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="32%" /></p>

## License

    Copyright (C) 2018 Hai Zhang

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
