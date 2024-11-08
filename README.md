# rn-native-media-picker

A React Native library for picking media (images and videos) from the native media library.

You can use this project as a sample module yourself. Remember to give feedback and contribute to improving it. If you think it is helpful, give me a star. Thank you so much!

## Notes

#### Android:

- Custom UI media picker that can handle READ_MEDIA_VISUAL_USER_SELECTED (Android 14 and above) with built-in request/check permissions.

| No | Permission                                         | Description                                         |
| -- | -------------------------------------------------- | --------------------------------------------------- |
| 01 | android.permission.READ_EXTERNAL_STORAGE           | Allows an application to read from external storage (API level 32 or lower) |
| 02 | android.permission.READ_MEDIA_IMAGES               | Allows an application to read image files from external storage (API level 33 or higher) |
| 03 | android.permission.READ_MEDIA_VIDEO                | Allows an application to read video files from external storage (API level 33 or higher) |
| 04 | android.permission.READ_MEDIA_VISUAL_USER_SELECTED | Allows an application to read image or video files from external storage that a user has selected via the permission prompt photo picker (API level 34 or higher), should be requested alongside 02 and 03 |

#### IOS:

- Just a simple media picker.

## Installation

```sh
npm install rn-native-media-picker
```

## Demo

#### Android:

https://github.com/user-attachments/assets/471749de-b3ef-459c-9890-12870fa0a700

#### IOS:

https://github.com/user-attachments/assets/533fdb64-0fbd-4844-a14f-0e07e76d6c85

## Usage

```js
import { launchLibrary, IOptions } from 'rn-native-media-picker';

// ...

const onLaunchLibrary = () => {
  const options: IOptions = {
    isMultipleSelection: false,
    maxSelection: 1,
    mediaType: 'image',
  };
  launchLibrary(options).then((result) =>
    console.log('result', result)
  );
};
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---
