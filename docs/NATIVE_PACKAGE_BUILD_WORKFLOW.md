# Floently Native Package Build Workflow

## Purpose

This document defines how future AI/human builders should receive files, build native apps, and send modified files back.

## Rule

Do not patch the old Expo app for new native work.

Use:

- /root/floently-native/apps/ios
- /root/floently-native/apps/android
- /root/floently-native/shared
- /root/floently-native/docs

## Package style

Each work package should be separated by purpose:

1. Native shared API contracts
2. Native iOS Learn
3. Native Android Learn
4. Native iOS Read
5. Native Android Read
6. Native Create shell
7. Backend API contract/support files
8. Design system/assets

## Returned output

External builders should return:

- modified-files zip
- apply script
- manifest
- build notes
- test notes
- known gaps

## Apply rule

Never apply directly into production repo without inspection.

Always apply to a clean isolated workspace first.

## Backend rule

Backend remains shared.

YKI/card bank remains backend source of truth.

Native apps consume APIs.
