import { useState, useEffect } from 'react';
import { StyleSheet, View, Text, Button, Image } from 'react-native';
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
      if (results.resultCode === 0) {
        setResults(results.assets);
      } else {
        alert('Error: ' + results.resultCode);
      }
    });
  };

  return (
    <View style={styles.container}>
      <Button title={'Launch'} onPress={() => onPress1()} />
      {results.map((result, index) => (
        <View key={`result-${index}`} style={styles.row}>
          <Image style={styles.image} source={{ uri: result.mediaUri}} />
          <Text style={styles.url}>{result.mediaUri}</Text>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    alignItems: 'center',
    justifyContent: 'flex-start',
  },
  row: {
    flexDirection: 'row',
    paddingVertical: 25,
    paddingHorizontal: 50,
    alignItems: 'center',
  },
  image: {
    aspectRatio: 1,
    width: 100,
  },
  url: {
    padding: 20,
    fontSize: 16,
    color: '#FFFFFF',
  },
});
