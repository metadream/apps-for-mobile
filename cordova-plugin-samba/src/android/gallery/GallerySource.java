package net.cloudseat.smbova;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;

import java.io.IOException;

public abstract class GallerySource {

    /**
     * 子类必须实现的抽象方法
     */
    // 当前图片索引
    protected abstract int currentIndex();
    // 所有图片数量
    protected abstract int size();
    // 图片缓存键值
    protected abstract String key(int index);
    // 图片数据
    protected abstract byte[] data(int index) throws IOException;

    /**
     * 图像内存缓存
     * 使用静态代码块防止重复创建
     */
    private static LruCache<String, Bitmap> imageCache;
    static {
        // 应用程序最大可用内存
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // 将最大可用内存的 1/5 作为图像缓存
        final int cacheSize = maxMemory / 5;

        imageCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    /**
     * 根据索引加载图像
     * @param int index 图像索引
     * @param OnImageLoadedListener listener 加载完成后回调
     */
    public void load(int index, OnImageLoadedListener listener) {
        Bitmap bitmap = imageCache.get(key(index));
        if (bitmap != null) {
            listener.onImageLoaded(bitmap);
        } else {
            new ImageLoader(listener).execute(index);
        }
    }

    ///////////////////////////////////////////////////////
    // 私有类
    ///////////////////////////////////////////////////////

    /**
     * 图像加载异步任务
     * Android 不允许在主线程（UI线程）请求网络，否则抛出 NetworkOnMainThreadException
     * 故开启新的任务线程获取图片数据
     */
    private class ImageLoader extends AsyncTask<Integer, Integer, Bitmap> {

        // 加载完成后回调
        private OnImageLoadedListener listener;
        public ImageLoader(OnImageLoadedListener listener) {
            this.listener = listener;
        }

        // 执行任务：在子线程执行，不允许更新UI
        @Override
        protected Bitmap doInBackground(Integer... params) {
            try {
                int index = params[0];
                byte[] bytes = data(index);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                // 加入缓存
                imageCache.put(key(index), bitmap);
                return bitmap;
            } catch (IOException e) {
                return null;
            }
        }

        // 任务完成后回调：在主线程执行，允许更新UI
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            listener.onImageLoaded(bitmap);
        }
    }

    ///////////////////////////////////////////////////////
    // 回调接口
    ///////////////////////////////////////////////////////

    /**
     * 图像加载完成后回调
     * 需实现 onImageLoaded 方法
     */
    public interface OnImageLoadedListener {
        public void onImageLoaded(Bitmap bitmap);
    }

}
