# rn-native-media-picker

A React Native library for picking media (images and videos) from the native media library.

It can handle the new media picker on Android 14 (READ_MEDIA_VISUAL_USER_SELECTED). If you think it is helpful, give me a star and I will improve it in the future. Thank you so much!

## Notes

The first version is a simple Android function that requests permissions and selects media from the native UI library.

## Installation

```sh
npm install rn-native-media-picker
```

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
