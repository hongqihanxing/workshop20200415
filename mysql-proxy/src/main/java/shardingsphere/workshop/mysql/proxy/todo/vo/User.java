package shardingsphere.workshop.mysql.proxy.todo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 用户信息
 *
 * @author zhanghongqi21
 * @create 2020-05-03 13:42
 **/
@Data
@Builder
public class User {

  private Integer id;

  private String name;
}
