class EventSources {
    //构造函数
    constructor(url, onmessage) {
        this.eventSourceUrl = url;
        this.onmessage = onmessage;
        this.eventSource = null;
        this.initEventSource();
    }

    //初始化eventSource
    initEventSource() {
    
        //兼容判断
        if ('EventSource' in window) {

            let that = this;

            //实例化EventSource
                this.eventSource = new EventSource(that.eventSourceUrl);
            //EventSource打开
            this.eventSource.onopen = function () {
                console.log('EventSource连接成功', that.eventSourceUrl);
            };

            //EventSource接收到新消息
            this.eventSource.onmessage = function (event) {
                try {
                    if (event.data && typeof event.data === 'string') {
                        
                        let data = JSON.parse(event.data);
                        
                        //业务逻辑回调
                        if (typeof that.onmessage === 'function') {
                            that.onmessage(data);
                        }
                    }
                } catch (error) {
                    
                    console.log('EventSource初始化异常', error);
                }
            };

            //EventSource关闭
            this.eventSource.onclose = function () {
                console.log('EventSource连接断开', that.eventSourceUrl);
            };

            //EventSource错误
            this.eventSource.onerror = function (error) { // 监听错误
                console.log('EventSource连接错误', error);
                
                this.close();
            }
        
        }
        else {
            throw new Error('浏览器不支持EventSource对象')
        }
    }

    //关闭eventSource
    close() {
        this.eventSource.close();
        this.eventSourceUrl = '';
        this.eventSource = null;
        this.onmessage = null;
    }
}

export default EventSources
