/**
 * Samba Plugin
 * Copyright (c) 2019, CLOUDSEAT Inc.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * @author AiChen
 * @copyright (c) 2019, CLOUDSEAT Inc.
 * @license https://www.gnu.org/licenses
 * @link https://www.cloudseat.net
 */

package net.cloudseat.smbova;

import android.content.Context;
import android.content.Intent;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import com.greatape.bmds.BufferedMediaDataSource;

/**
 * Samba 插件类
 */
public class SambaPlugin extends CordovaPlugin {

    /**
     * 覆盖父类方法
     */
    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callback)
        throws JSONException {

        switch (action) {
            case "runBackground":
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                cordova.getActivity().startActivity(intent);
                callback.success();
                break;
            case "auth":
                String username = args.getString(0);
                String password = args.getString(1);
                SambaFile.setPrincipal(username, password);
                callback.success();
                break;
            case "listEntries": listEntries(args, callback); break;
            case "readAsText": readAsText(args, callback); break;
            case "readAsByteArray": readAsByteArray(args, callback); break;
            case "openImage": openImage(args, callback); break;
            case "openMedia": openMedia(args, callback); break;
            case "openFile": openFile(args, callback); break;
            case "upload": upload(args, callback); break;
            case "download": download(args, callback); break;
            case "createFile": createFile(args, callback); break;
            case "createDirectory": createDirectory(args, callback); break;
            case "delete": delete(args, callback); break;
            case "wakeOnLan": wakeOnLan(args, callback); break;
            default:
                callback.error("Undefined method:" + action);
                return false;
        }
        return true;
    }

    /**
     * 列出目录下文件夹和文件
     */
    private void listEntries(CordovaArgs args, CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SambaFile file = new SambaFile(args.getString(0));
                    callback.success(file.listEntries());
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    /**
     * 读取文本文件
     */
    private void readAsText(CordovaArgs args, CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SambaFile file = new SambaFile(args.getString(0));
                    callback.success(file.readAsText());
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    /**
     * 读取文件为字节数组
     */
    private void readAsByteArray(CordovaArgs args, CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SambaFile file = new SambaFile(args.getString(0));
                    callback.success(file.readAsByteArray());
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    /**
     * 创建文件
     */
    private void createFile(CordovaArgs args, CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SambaFile file = new SambaFile(args.getString(0));
                    callback.success(file.createFile());
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    /**
     * 创建文件夹
     */
    private void createDirectory(CordovaArgs args, CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SambaFile file = new SambaFile(args.getString(0));
                    callback.success(file.createDirectory());
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    /**
     * 删除文件夹或文件
     */
    private void delete(CordovaArgs args, CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SambaFile file = new SambaFile(args.getString(0));
                    file.delete();
                    callback.success();
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    /**
     * 上传文件
     */
    private void upload(CordovaArgs args, CallbackContext callback) throws JSONException {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String localPath = args.getString(0);
                    String smbPath = args.getString(1);

                    // 将路径解析为本地原生路径
                    Context context = cordova.getActivity().getApplicationContext();
                    String nativePath = NativePath.parse(context, localPath);
                    // 获取要上传文件的文件名
                    int index = nativePath.lastIndexOf("/");
                    String fileName = nativePath.substring(index + 1);

                    SambaFile smbFile = new SambaFile(smbPath + fileName);
                    JSONObject result = smbFile.upload(nativePath, new SambaFile.OnProgressListener() {
                        @Override
                        public void onProgress(float progress) {
                            webView.sendJavascript("window.samba.onProgress(" + progress + ")");
                        }
                    });
                    callback.success(result);
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    /**
     * 下载文件
     */
    private void download(CordovaArgs args, CallbackContext callback) throws JSONException {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String smbPath = args.getString(0);
                    String fileName = smbPath.substring(smbPath.lastIndexOf("/"));
                    SambaFile smbFile = new SambaFile(smbPath);
                    String localPath = getExternalStoragePath(smbFile.getGroupType()) + fileName;

                    smbFile.download(localPath, new SambaFile.OnProgressListener() {
                        @Override
                        public void onProgress(float progress) {
                            webView.sendJavascript("window.samba.onProgress(" + progress + ")");
                        }
                    });

                    // 更新系统相册
                    MediaScannerConnection.scanFile(
                        cordova.getActivity(),
                        new String[]{ localPath },
                        new String[]{ getMimeType(localPath) },
                        null
                    );
                    callback.success(localPath);
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    /**
     * 用本地应用打开文件
     */
    private void openFile(CordovaArgs args, CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = args.getString(0);
                    String mimeType = getMimeType(path);

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(Uri.parse(path), mimeType);
                    cordova.getActivity().startActivity(intent);

                    callback.success(mimeType);
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    /**
     * 打开图片
     */
    private void openImage(CordovaArgs args, CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = args.getString(0);
                    String parentPath = path.substring(0, path.lastIndexOf("/") + 1);
                    SambaFile directory = new SambaFile(parentPath);
                    List<SambaFile> imageFiles = directory.listImages();

                    GalleryActivity.gallerySource = new GallerySource() {
                        @Override
                        public int currentIndex() {
                            for (int i = 0; i < size(); i++) {
                                if (imageFiles.get(i).getPath().equals(path)) return i;
                            }
                            return 0;
                        }
                        @Override
                        public int size() {
                            return imageFiles.size();
                        }
                        @Override
                        public String key(int index) {
                            return imageFiles.get(index).getPath();
                        }
                        @Override
                        public byte[] data(int index) throws IOException {
                            return imageFiles.get(index).readAsByteArray();
                        }
                    };

                    Intent intent = new Intent(cordova.getActivity(), GalleryActivity.class);
                    cordova.getActivity().startActivity(intent);
                    callback.success();
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    /**
     * 打开音频或视频
     */
    private void openMedia(CordovaArgs args, CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = args.getString(0);
                    PlayerActivity.dataSource = createBufferedMediaDataSource(path);
                    PlayerActivity.timedTextFile = createTempSubtitleFile(path);

                    Intent intent = new Intent(cordova.getActivity(), PlayerActivity.class);
                    cordova.getActivity().startActivity(intent);
                    callback.success();
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    /**
     * 网络唤醒
     */
    private void wakeOnLan(CordovaArgs args, CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String mac = args.getString(0);
                    int port = args.getInt(1);
                    WakeOnLan.broadcast(mac, port);
                    callback.success();
                } catch (Exception e) {
                    callback.error(e.getMessage());
                }
            }
        });
    }

    ///////////////////////////////////////////////////////
    // 私有工具方法
    ///////////////////////////////////////////////////////

    /**
     * 根据文件路径获取 MimeType
     */
    private String getMimeType(String path) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(path).toLowerCase();
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    /**
     * 根据文件类型获取本地下载目录
     */
    private String getExternalStoragePath(int groupType) {
        String externalStoragePath = "";
        if (groupType == SambaFile.GROUP_IMAGE) {
            externalStoragePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        } else
        if (groupType == SambaFile.GROUP_AUDIO) {
            externalStoragePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString();
        } else {
        if (groupType == SambaFile.GROUP_VIDEO) {
            externalStoragePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
        } else
            externalStoragePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        }
        return externalStoragePath;
    }

    /**
     * 创建缓存媒体数据源
     */
    private BufferedMediaDataSource createBufferedMediaDataSource(String path) throws IOException {
        SambaFile file = new SambaFile(path);
        return new BufferedMediaDataSource(new BufferedMediaDataSource.StreamCreator() {
            @Override
            public InputStream openStream() throws IOException {
                return file.getInputStream();
            }
            @Override
            public long length() throws IOException {
                return file.length();
            }
            @Override
            public String typeName() {
                return "SmbFile";
            }
        });
    }

    /**
     * 根据视频文件名获取当前目录下同名字幕文件
     * 如果存在则复制到当前应用的缓存目录
     */
    private String createTempSubtitleFile(String path) throws IOException {
        int i = path.lastIndexOf("/") + 1;
        int j = path.lastIndexOf(".");
        String dir = path.substring(0, i);
        String name = i > j ? path.substring(i) : path.substring(i, j);
        String expectedSubtitle = dir + name + ".srt";
        String tempSubtitle = cordova.getActivity().getCacheDir() + "/" + "temp.srt";

        try {
            FileOutputStream fos = new FileOutputStream(tempSubtitle);
            OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
            SambaFile file = new SambaFile(expectedSubtitle);
            writer.write(file.readAsText());
            writer.flush();
            writer.close();
            return tempSubtitle;
        } catch (IOException e) {
            return null;
        }
    }

}
