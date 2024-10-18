import { useState, useEffect } from 'react';
import { StyleSheet, View, Text, Button } from 'react-native';
import { multiply, launchLibrary, IOptions } from 'rn-native-media-picker';
import { IAsset, IResponse } from "../../src/type";

export default function App() {
  const [results, setResults] = useState<IAsset[]>([]);

  const onPress1 = () => {
    const options: IOptions = {
      isMultipleSelection: false,
      maxSelection: 1,
      mediaType: 'image',
    };
    launchLibrary(options).then((results: IResponse) => {
      console.log('results', results);
      setResults(results.assets);
    });
  };

  return (
    <View style={styles.container}>
      {results.map((result, index) => (
        <Text key={`result-${index}`}>
          {result?.mediaUri}
        </Text>
      ))}
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
