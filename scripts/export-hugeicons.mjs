// Exports the official free icon geometry for the native Compose design system.
// Usage: node scripts/export-hugeicons.mjs <extracted core-free-icons directory>
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const icons = await import(pathToFileURL(resolve(process.argv[2], 'dist/esm/index.js')));
const mapping = {
  Home: 'Home01Icon', Explore: 'Compass01Icon', Download: 'Download04Icon',
  FolderOpen: 'FolderLibraryIcon', PersonOutline: 'UserCircleIcon', Search: 'Search01Icon',
  ContentPaste: 'ClipboardPasteIcon', PlayArrow: 'PlayIcon', Pause: 'PauseIcon',
  Cancel: 'Cancel01Icon', Close: 'Cancel01Icon', Check: 'Tick01Icon',
  CheckCircle: 'CheckmarkCircle02Icon', DarkMode: 'Moon02Icon', Dns: 'ServerStack01Icon',
  FileOpen: 'File01Icon', Folder: 'Folder01Icon', FolderCopy: 'FolderLibraryIcon',
  Gif: 'Gif01Icon', Image: 'Image01Icon', Info: 'InformationCircleIcon',
  ErrorOutline: 'AlertCircleIcon', Wifi: 'Wifi01Icon', Share: 'Share01Icon',
  Headphones: 'HeadphonesIcon', Audiotrack: 'MusicNote01Icon', MusicNote: 'MusicNote01Icon',
  Album: 'Album01Icon', CameraAlt: 'Camera01Icon', Cloud: 'CloudIcon',
  MoreVert: 'MoreVerticalIcon', Refresh: 'ArrowReloadHorizontalIcon', Language: 'Globe02Icon',
  DeleteOutline: 'Delete02Icon', DeleteSweep: 'Delete01Icon', ArrowForward: 'ArrowRight01Icon',
  ArrowBack: 'ArrowLeft01Icon', ChevronRight: 'ArrowRight01Icon', PlayCircle: 'PlayCircleIcon',
  SmartDisplay: 'Video01Icon', VideoLibrary: 'VideoReplayIcon', Movie: 'Film01Icon',
  Collections: 'Image01Icon', Facebook: 'Facebook01Icon', PushPin: 'PinIcon',
  Forum: 'Chatting01Icon', Speed: 'DashboardSpeed01Icon',
  Youtube: 'YoutubeIcon', Instagram: 'InstagramIcon', Tiktok: 'TiktokIcon',
  Reddit: 'RedditIcon', Pinterest: 'PinterestIcon', Soundcloud: 'SoundcloudIcon', Twitter: 'NewTwitterIcon'
};
const result = {};
for (const [name, source] of Object.entries(mapping)) {
  if (!icons[source]) throw new Error(`Missing Hugeicons export ${source}`);
  result[name] = { source, geometry: icons[source] };
}
process.stdout.write(JSON.stringify(result));
