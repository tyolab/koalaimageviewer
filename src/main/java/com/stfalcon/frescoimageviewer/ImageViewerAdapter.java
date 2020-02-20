package com.stfalcon.frescoimageviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.stfalcon.frescoimageviewer.adapter.RecyclingPagerAdapter;
import com.stfalcon.frescoimageviewer.adapter.ViewHolder;
import com.stfalcon.frescoimageviewer.drawee.ZoomableDraweeView;

import java.util.HashSet;

import me.relex.photodraweeview.OnScaleChangeListener;

/*
 * Created by troy379 on 07.12.16.
 */
public class ImageViewerAdapter
        extends RecyclingPagerAdapter<ImageViewerAdapter.ImageViewHolder> {

    public static final int IMAGE_VIEW_TYPE_PLAIN = 0;
    public static final int IMAGE_VIEW_TYPE_DRAWEE = 1;

    private Context context;
    private ImageViewer.DataSet<?> dataSet;
    private HashSet<ImageViewHolder> holders;
    private ImageRequestBuilder imageRequestBuilder;
    private GenericDraweeHierarchyBuilder hierarchyBuilder;
    private boolean isZoomingAllowed;

    private int imageViewType;

    public ImageViewerAdapter(Context context, ImageViewer.DataSet<?> dataSet,
                              ImageRequestBuilder imageRequestBuilder,
                              GenericDraweeHierarchyBuilder hierarchyBuilder,
                              boolean isZoomingAllowed) {
        this.context = context;
        this.dataSet = dataSet;
        this.holders = new HashSet<>();
        this.imageRequestBuilder = imageRequestBuilder;
        this.hierarchyBuilder = hierarchyBuilder;
        this.isZoomingAllowed = isZoomingAllowed;

        this.imageViewType = IMAGE_VIEW_TYPE_DRAWEE;
    }

    public int getImageViewType() {
        return imageViewType;
    }

    public void setImageViewType(int imageViewType) {
        this.imageViewType = imageViewType;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageViewHolder holder = null;
        if (imageViewType == IMAGE_VIEW_TYPE_DRAWEE) {
            ZoomableDraweeView drawee = new ZoomableDraweeView(context);
            drawee.setEnabled(isZoomingAllowed);
            holder = new DraweeViewHolder(drawee);
        }
        else {
            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder = new PlainImageViewHolder(imageView);
        }

        holders.add(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return dataSet.getData().size();
    }


    boolean isScaled(int index) {
        for (ImageViewHolder holder : holders) {
            if (holder.position == index) {
                return holder.isScaled;
            }
        }
        return false;
    }

    void resetScale(int index) {
        for (ImageViewHolder holder : holders) {
            if (holder.position == index) {
                holder.resetScale();
                break;
            }
        }
    }

    String getUrl(int index) {
        return dataSet.format(index);
    }

    private BaseControllerListener<ImageInfo>
    getDraweeControllerListener(final ZoomableDraweeView drawee) {
        return new BaseControllerListener<ImageInfo>() {
            @Override
            public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                super.onFinalImageSet(id, imageInfo, animatable);
                if (imageInfo == null) {
                    return;
                }
                drawee.update(imageInfo.getWidth(), imageInfo.getHeight());
            }
        };
    }

    abstract class ImageViewHolder extends ViewHolder implements OnScaleChangeListener {

        private int position = -1;

        protected boolean isScaled;

        ImageViewHolder(View itemView) {
            super(itemView);

        }

        void bind(int position) {
            this.position = position;

        }

        abstract void resetScale();
    }

    public class PlainImageViewHolder extends ImageViewHolder {

        private ImageView imageView;

        PlainImageViewHolder(View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView;
        }

        @Override
        void resetScale() {

        }

        @Override
        public void onScaleChange(float scaleFactor, float focusX, float focusY) {

        }

        @Override
        void bind(int position) {
            super.bind(position);

            Bitmap bitmap = (Bitmap) dataSet.getData().get(position);
            imageView.setImageBitmap(bitmap);
        }
    }

    public class DraweeViewHolder extends ImageViewHolder {

        private ZoomableDraweeView drawee;

        DraweeViewHolder(View itemView) {
            super(itemView);

            drawee = (ZoomableDraweeView) itemView;
        }

        @Override
        void bind(int position) {
            super.bind(position);

            tryToSetHierarchy();
            setController(dataSet.format(position));

            drawee.setOnScaleChangeListener(this);
        }

        @Override
        public void onScaleChange(float scaleFactor, float focusX, float focusY) {
            isScaled = drawee.getScale() > 1.0f;
        }

        @Override
        void resetScale() {
            drawee.setScale(1.0f, true);
        }

        private void tryToSetHierarchy() {
            if (hierarchyBuilder != null) {
                hierarchyBuilder.setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
                drawee.setHierarchy(hierarchyBuilder.build());
            }
        }

        private void setController(String url) {
            PipelineDraweeControllerBuilder controllerBuilder = Fresco.newDraweeControllerBuilder();
            controllerBuilder.setUri(url);
            controllerBuilder.setOldController(drawee.getController());
            controllerBuilder.setControllerListener(getDraweeControllerListener(drawee));
            if (imageRequestBuilder != null) {
                imageRequestBuilder.setSource(Uri.parse(url));
                controllerBuilder.setImageRequest(imageRequestBuilder.build());
            }
            drawee.setController(controllerBuilder.build());
        }
    }
}
