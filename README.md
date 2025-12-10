1、项目采用kt语言

2、项目主界面文件为MainActivity.kt，界面文件为activity_main.xml,主界面包括公共相册，拍照，以及私有相册（未实现）

3、公共相册文件为PublicAlbumActivity，相册内部图片间布局文件为GridSpacingItemDecoration，界面文件为activity_public_album.xml文件

4、对于点击图片查看细节文件为ImageDetailActivity文件，界面文件为activity_image_detail.xml，文件细节中包含对文件的编辑按钮，已实现剪裁，综合，文字

  4.1、对于图片细节放大功能为ZoomableImageView文件

5、剪裁功能模块文件为NewCropActivity，布局文件为activity_crop_new.xml。剪裁功能包括图片剪裁和图片旋转两部分

6、综合功能包含亮度，对比度，滤镜，贴纸，拼接（未实现）功能。综合功能模块文件未ImageEnhanceActivity,界面文件为activity_image_enhance.xml文件

  6.1、亮度、对比度功能内嵌入ImageEnhanceActivity文件中，滤镜和贴纸功能在该文件中进行调用

  6.2、FilterUtils作为滤镜工具类，实现了六种滤镜功能

  6.3、StickerManager.kt对贴纸资源进行管理；StickerOverlayView.kt是编辑贴纸的活动文件，支持贴纸的旋转、放大、删除以及图层变换

7、TextEditActivity.kt实现文字编辑功能，对字体样式、大小以及颜色进行设置。TextOverlayView.kt文件是对文字的放大、旋转等操作进行实现。文字编辑界面文件为activity_text_edit.xml文件

8、Photo.kt文件用于区分图片数据类型；PhotoAdapter.kt作为图片适配器，支持不同的图片来源以及空位置的图片占位，并采用Glide对不同加载缓存；
ImageManager.kt文件功能为用户图片的保存读取，对图片信息进行更新维护

9、公共相册中系统图片以及贴纸资源图片位于drawable文件夹下，同时设计了app图标。

  

