/**
 * Samba File
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

import jcifs.smb.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.MalformedURLException;
import java.text.Collator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class SambaFile extends SmbFile {

    public static final int GROUP_IMAGE = 1;
    public static final int GROUP_AUDIO = 2;
    public static final int GROUP_VIDEO = 3;

    private static final Map<Integer, String> GROUP_TYPES = new HashMap<Integer, String>() {{
        put(GROUP_IMAGE, "bmp,cur,eps,gif,ico,jpe,jpg,jpeg,jpz,png,svg,tif,tiff");
        put(GROUP_AUDIO, "aac,aiff,ape,caf,flac,m3u,m4a,mp3,ogg,wav,wma");
        put(GROUP_VIDEO, "3gp,asf,avi,flv,m3u8,m4u,m4v,mkv,mov,mp4,mpa,mpe,mpeg,mpg,ogm,rm,rmvb,vob,webm,wmv");
    }};

    private static final int BUFFER_SIZE = 8192;
    private static NtlmPasswordAuthentication auth;

    // 中文排序
    private Collator collator = Collator.getInstance(Locale.CHINESE);

    /**
     * 重载构造器
     * @param String path
     */
    public SambaFile(String path) throws MalformedURLException {
        super(path, auth);
    }

    /**
     * Sets username and password authentication
     * @param String username
     * @param String password
     * @return
     */
    public static void setPrincipal(String username, String password) {
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            auth = new NtlmPasswordAuthentication(null, username, password);
        } else {
            auth = null;
        }
    }

    /**
     * 根据当前路径列出所有远程文件夹和文件
     * 由于返回值不同，不能与父类方法重名
     * @return JSONArray
     */
    public JSONArray listEntries() throws MalformedURLException, SmbException, JSONException {
        if (this.exists() && this.isDirectory()) {
            SmbFile[] files = this.listFiles();
            List<JSONObject> list = parseToList(files);
            Collections.sort(list, new SambaComparator());
            return new JSONArray(list);
        }
        return null;
    }

    /**
     * 获取当前路径下所有图片文件
     * @return List<SambaFile>
     */
    public List<SambaFile> listImages() throws MalformedURLException, SmbException {
        List<SambaFile> list = new ArrayList<SambaFile>();
        if (this.exists() && this.isDirectory()) {
            String[] names = this.list();

            Collections.sort(Arrays.asList(names), new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return collator.compare(s1, s2);
                }
            });

            for (String name : names) {
                int groupType = getGroupType(parseExtName(name));
                if (groupType == SambaFile.GROUP_IMAGE) {
                    list.add(new SambaFile(this.getPath() + name));
                }
            }
        }
        return list;
    }

    /**
     * 创建空文件夹
     * @return JSONObject
     */
    public JSONObject createDirectory() throws MalformedURLException, SmbException, JSONException {
        this.mkdir();

        JSONObject entry = new JSONObject();
        entry.put("name", parseName(this.getName()));
        entry.put("type", parseType(this));
        entry.put("path", this.getPath());
        entry.put("size", 0);
        entry.put("lastModified", System.currentTimeMillis());
        return entry;
    }

    /**
     * 创建空文件
     * @return JSONObject
     */
    public JSONObject createFile() throws MalformedURLException, SmbException, JSONException {
        this.createNewFile();

        JSONObject entry = new JSONObject();
        entry.put("name", parseName(this.getName()));
        entry.put("ext", parseExtName(this.getName()));
        entry.put("type", parseType(this));
        entry.put("path", this.getPath());
        entry.put("size", 0);
        entry.put("lastModified", System.currentTimeMillis());
        return entry;
    }

    /**
     * 读取文件内容为字节数组
     * @return byte[]
     */
    public byte[] readAsByteArray() throws IOException {
        InputStream in = this.getInputStream();
        byte[] bytes = new byte[(int) this.length()];
        in.read(bytes);
        in.close();
        return bytes;
    }

    /**
     * 读取文件内容为字符串
     * @param path
     * @return String
     */
    public String readAsText() throws IOException {
        return new String(readAsByteArray(), "UTF-8");
    }

    /**
     * 上传本地文件到远程路径
     * @param String localPath
     * @param OnProgressListener listener
     * @return JSONObject
     */
    public JSONObject upload(String localPath, OnProgressListener listener)
        throws IOException, JSONException {

        File file = new File(localPath);
        FileInputStream in = new FileInputStream(file);
        OutputStream out = this.getOutputStream();

        long totalSize = file.length();
        long size = 0;
        byte[] b = new byte[BUFFER_SIZE];
        int len = 0;
        while((len = in.read(b)) > 0) {
            out.write(b, 0, len);
            size += len;
            listener.onProgress((float) size / totalSize);
        }
        in.close();
        out.close();

        JSONObject entry = new JSONObject();
        entry.put("name", parseName(this.getName()));
        entry.put("ext", parseExtName(this.getName()));
        entry.put("type", parseType(this));
        entry.put("path", this.getPath());
        entry.put("size", this.length());
        entry.put("lastModified", System.currentTimeMillis());
        return entry;
    }

    /**
     * 将远程文件下载到本地
     * @param String localPath
     * @param OnProgressListener listener
     * @return JSONObject
     */
    public void download(String localPath, OnProgressListener listener)
        throws IOException, JSONException {
        InputStream in = this.getInputStream();
        FileOutputStream out = new FileOutputStream(localPath);

        long totalSize = this.length();
        long size = 0;
        byte[] b = new byte[BUFFER_SIZE];
        int len = 0;
        while((len = in.read(b)) > 0) {
            out.write(b, 0, len);
            size += len;
            listener.onProgress((float) size / totalSize);
        }
        in.close();
        out.close();
    }

    /**
     * 根据文件名后缀获取文件类型分组
     * @return int
     */
    public int getGroupType() {
        return getGroupType(parseExtName());
    }

    /**
     * 获取文件名后缀
     * @return String
     */
    public String parseExtName() {
        return parseExtName(this.getName());
    }

    ///////////////////////////////////////////////////////
    // 私有方法
    ///////////////////////////////////////////////////////

    /**
     * Parses smbfiles to arraylist
     * @param SmbFile[] files
     * @return List<JSONObject>
     */
    private List<JSONObject> parseToList(SmbFile[] files) throws SmbException, JSONException {
        List<JSONObject> list = new ArrayList<JSONObject>();
        for (SmbFile file : files) {
            int type = file.getType();
            if (type != SmbFile.TYPE_FILESYSTEM && type != SmbFile.TYPE_SHARE) {
                continue;
            }
            String name = file.getName();
            if (name.endsWith("$/")) {
                continue;
            }
            JSONObject entry = new JSONObject();
            entry.put("name", parseName(name));
            entry.put("ext", parseExtName(name));
            entry.put("type", parseType(file));
            entry.put("path", file.getPath());
            entry.put("size", file.length());
            entry.put("lastModified", file.getLastModified());
            list.add(entry);
        }
        return list;
    }

    /**
     * Trims the '/' in the end.
     * @param String name
     * @return String
     */
    private String parseName(String name) {
        return name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
    }

    /**
     * Gets default type excerpt file
     * @param SmbFile file
     * @return int 0-file, 1-directory, 4-server, 5-share
     */
    private int parseType(SmbFile file) throws SmbException {
        return file.isFile() ? 0 : file.getType();
    }

    /**
     * 根据后缀获取类型组
     * @param String extname
     * @return int
     */
    private int getGroupType(String extname) {
        for (Map.Entry<Integer, String> entry : GROUP_TYPES.entrySet()) {
            String[] exts = entry.getValue().split(",");
            for (String ext : exts) {
                if (ext.equalsIgnoreCase(extname)) return entry.getKey();
            }
        }
        return 0;
    }

    /**
     * 获取文件名后缀
     * @param String name
     * @return String
     */
    private String parseExtName(String name) {
        int index = name.lastIndexOf(".");
        return index > -1 ? name.substring(index + 1).toLowerCase() : "";
    }

    ///////////////////////////////////////////////////////
    // 私有类
    ///////////////////////////////////////////////////////

    /**
     * 列表排序类
     */
    private class SambaComparator implements Comparator<JSONObject> {
        @Override
        public int compare(JSONObject o1, JSONObject o2) {
            try {
                int t1 = o1.getInt("type");
                int t2 = o2.getInt("type");
                if (t1 != t2) return t2 - t1;
                return collator.compare(o1.getString("name"), o2.getString("name"));
            } catch (JSONException e) {
                return 0;
            }
        }
    }

    ///////////////////////////////////////////////////////
    // 公共接口
    ///////////////////////////////////////////////////////

    /**
     * 文件传输（上传下载）进度回调接口
     * 需实现 onProgress 方法
     */
    public interface OnProgressListener {
        public void onProgress(float progress);
    }

}
