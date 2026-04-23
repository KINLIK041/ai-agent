from moviepy.editor import VideoFileClip

# 要转换的 MP4 文件列表
videos = ["demo1.mp4", "demo2.mp4"]

for video_path in videos:
    # 读取视频
    clip = VideoFileClip(video_path)
    # 设置输出文件名
    output_path = video_path.replace(".mp4", ".gif")
    # 生成 GIF，设置帧率为 10（文件更小）
    clip.write_gif(output_path, fps=10)
    # 关闭视频剪辑以释放内存
    clip.close()
    print(f"{video_path} 已成功转换为 {output_path}")

print("所有视频转换完成！")