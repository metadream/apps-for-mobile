package net.cloudseat.smbova;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import java.util.LinkedList;
import net.cloudseat.smbova.R;

public class GalleryActivity extends Activity {

    // 接受外部图像数据源传参
    public static GallerySource gallerySource;

    // ViewPager Item 页面缓存
    private LinkedList<View> itemViewCache = new LinkedList<View>();

    /**
     * 覆盖父类创建方法
     * @param Bundle savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_viewer);

        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setPageMargin(50);
        viewPager.setAdapter(new GalleryAdapter());
        viewPager.setCurrentItem(gallerySource.currentIndex());
    }

    ///////////////////////////////////////////////////////
    // 内部类
    ///////////////////////////////////////////////////////

    /**
     * 图片滑动翻页控件适配器
     */
    private class GalleryAdapter extends PagerAdapter {

        // 获取要滑动的控件（图片）数量
        @Override
        public int getCount() {
            return gallerySource.size();
        }

        // 官方建议直接返回两个参数相等
        @Override
        public boolean isViewFromObject(View v, Object o) {
            return v == o;
        }

        // 预加载需要显示的图片（默认最多三张）进行布局初始化（通常用于设置缩略图）
        // 需要将显示的 ImageView 加入到 ViewGroup 中，然后返回该值
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView;
            if (itemViewCache.size() > 0) {
                // 从缓存获取 item 布局
                itemView = itemViewCache.remove();
            } else {
                // 从 xml 获取 item 布局
                LayoutInflater inflater = (LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.gallery_item, null);
            }

            // 获取 item 布局中的控件并初始化
            ProgressBar loading = (ProgressBar) itemView.findViewById(R.id.loading);
            PinchImageView imageView = (PinchImageView) itemView.findViewById(R.id.image_view);
            imageView.setImageBitmap(null);
            imageView.reset();
            loading.setVisibility(View.VISIBLE);

            // 加载图像
            gallerySource.load(position, new GallerySource.OnImageLoadedListener() {
                @Override
                public void onImageLoaded(Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                    loading.setVisibility(View.INVISIBLE);
                }
            });

            // 加入到容器并返回
            container.addView(itemView);
            return itemView;
        }

        // 滑动的图片超出缓存范围（最多三张）会调用此方法将图片销毁
        // 需要将对应的 ImageView 从 ViewGroup 中移除
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View item = (View) object;
            container.removeView(item);
            itemViewCache.add(item);
        }
    }

}
