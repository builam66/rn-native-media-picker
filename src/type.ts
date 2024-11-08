export interface IOptions {
  mediaType: 'all' | 'image' | 'video';
  isMultipleSelection: boolean;
  maxSelection: number;
}

export interface IResponse {
  resultCode: number;
  assets: IAsset[];
}

export interface IAsset {
  uri: string;
  type: string;
  mimeType: string;
  name: string;
  size: number;
  width: number | undefined;
  height: number | undefined;
  datetime: string | undefined;
  duration: number | undefined;
  bitrate: number | undefined;
}
