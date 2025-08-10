package com.chatapp.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.chatapp.backend.model.ChatMember;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Integer> {

    List<ChatMember> findByUserId(Integer userId);

    List<ChatMember> findByChat_IdAndUser_IdNot(Integer chatId, Integer userId);;

    ChatMember findByChat_IdAndUser_Id(Integer chatId, Integer userId);
}
