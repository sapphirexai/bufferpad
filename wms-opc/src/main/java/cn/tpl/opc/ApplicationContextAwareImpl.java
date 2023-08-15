package cn.tpl.opc;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/8/15
 */
@Component
public class ApplicationContextAwareImpl implements ApplicationContextAware {
    private static ApplicationContext mContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        mContext = applicationContext;
    }


    /**
     * 根据beanName获取bean
     *
     * @param beanName bean名称
     * @return bean对象
     */
    public static Object getBean(String beanName) {
        return mContext.getBean(beanName);
    }
}
