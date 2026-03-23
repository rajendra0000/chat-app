package com.chatapp.backend.repository;

import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chatapp.backend.model.ChatMember;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Integer> {

    List<ChatMember> findByUserId(Integer userId);

    List<ChatMember> findByChat_IdAndUser_IdNot(Integer chatId, Integer userId);

    @Query("SELECT cm FROM ChatMember cm JOIN FETCH cm.chat WHERE cm.chat.id = :chatId AND cm.user.id = :userId")
    ChatMember findByChat_IdAndUser_Id(@Param("chatId") Integer chatId, @Param("userId") Integer userId);
    // ChatMember findByChat_IdAndUser_Id(Integer chatId, Integer userId);

    List<ChatMember> findByChat_Id(Integer chatId);

    boolean existsByChat_IdAndUser_Id(Integer chatId, Integer userId);

    void deleteByChat_IdAndUser_Id(Integer chatId, Integer userId);

}
