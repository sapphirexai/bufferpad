FROM nginx

MAINTAINER GavinRowe<luoguowen123@qq.com> #定义作者信息

RUN rm /etc/nginx/conf.d/default.conf

# 删除基础镜像中的Nginx配置文件替换成自己写的
ADD default.conf /etc/nginx/conf.d/

# 将打包好的vue项目复制到基础镜像中Nginx配置文件的指定路径下
COPY dist/ /usr/share/nginx/html/
