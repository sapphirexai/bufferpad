package cn.tpl.opc.auth;

import lombok.Value;
import java.util.List;

@Value
public class AuthSessionsRevoked {
    List<String> sessionHashes;
}
