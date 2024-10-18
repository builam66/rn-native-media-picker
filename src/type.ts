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
  mediaUri: string;
  mediaType: string;
}
