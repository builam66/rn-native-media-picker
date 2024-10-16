# react-native-media-picker

A React Native library for picking media (images and videos) from the native media library.

## Installation

```sh
npm install react-native-media-picker
```

## Usage


```js
import { launchLibrary, IOptions } from 'react-native-media-picker';

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
