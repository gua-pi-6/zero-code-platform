package com.chen.common;

import lombok.Data;

import java.io.Serializable;

@Data // 使用Lombok的@Data注解，自动生成getter、setter、toString、equals和hashCode方法
public class DeleteRequest implements Serializable { // 实现Serializable接口，使对象可以被序列化

    /**
     * id属性，用于标识需要删除的对象的唯一标识符
     * 使用多行注释说明该字段的作用
     */
    private Long id; // 定义一个Long类型的id属性，用于存储对象ID



    /**
     * 序列化版本UID，用于版本控制
     * 当类结构发生变化时，用于验证序列化和反序列化的对象是否兼容
     */
    private static final long serialVersionUID = 1L; // 定义序列化版本号，为1L
}
