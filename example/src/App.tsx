import { useState, useEffect } from 'react';
import { StyleSheet, View, Text, Button, Image, Platform } from 'react-native';
import { multiply, launchLibrary, IOptions } from 'rn-native-media-picker';
import { IAsset, IResponse } from "../../src/type";

export default function App() {
  const [results, setResults] = useState<IAsset[]>([]);

  const onPress1 = () => {
    const options: IOptions = {
      isMultipleSelection: true,
      maxSelection: 10,
      mediaType: 'all',
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
          <Image resizeMode={"center"} style={styles.image} source={{ uri: result.uri}} />
          <View style={styles.infoView}>
            <Text style={styles.url}>name: {result.name}</Text>
            <Text style={styles.url}>type: {result.type}</Text>
            <Text style={styles.url}>mimeType: {result.mimeType}</Text>
            <Text style={styles.url}>size: {result.size}</Text>
            <Text style={styles.url}>width: {result.width}</Text>
            <Text style={styles.url}>height: {result.height}</Text>
            <Text style={styles.url}>datetime: {result.datetime}</Text>
            <Text style={styles.url}>duration: {result.duration}</Text>
            <Text style={styles.url}>bitrate: {result.bitrate}</Text>
          </View>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    paddingTop: Platform.OS === 'ios' ? 50 : 20,
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
    width: 200,
  },
  infoView: {
    flexDirection: 'column',
  },
  url: {
    paddingLeft: 10,
    fontSize: 16,
    color: 'red',
  },
});
