import { useState, useEffect } from 'react';
import { StyleSheet, View, Text, Button } from 'react-native';
import { multiply, launchLibrary, IOptions } from 'react-native-media-picker';

export default function App() {
  const [result, setResult] = useState<number | undefined>();

  const onPress1 = () => {
    const options: IOptions = {
      isMultipleSelection: false,
      maxSelection: 1,
      mediaType: 'image',
    };
    launchLibrary(options).then((callback) =>
      console.log('callback1', callback)
    );
  };

  useEffect(() => {
    multiply(3, 7).then(setResult);
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
      <Button title={'Launch lib'} onPress={() => onPress1()} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
