document.addEventListener('DOMContentLoaded', () => {
    // --- DOM Elements ---
    const chatList = document.getElementById('chat-list');
    const chatWindow = document.getElementById('chat-window');
    const welcomeScreen = document.getElementById('welcome-screen');
    const messagesList = document.getElementById('messages-list');
    const messageInput = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-btn');
    const newChatBtn = document.getElementById('new-chat-btn');
    const newChatModal = new bootstrap.Modal(document.getElementById('newChatModal'));
    const searchUsersInput = document.getElementById('search-users-input');
    const searchResults = document.getElementById('search-results');
    const sidebar = document.getElementById('sidebar');
    const sidebarToggleBtn = document.getElementById('sidebar-toggle-btn');

    // --- State ---
    let currentUserId = null; // Will be set to phone from fetchCurrentUser
    let conversations = [];
    let activeConversationId = null;
    let stompClient = null;
    let currentSubscription = null;

    // --- API Endpoints ---
    const API = {
        fetchCurrentUser: '/api/users/me',
        fetchConversations: '/api/conversations',
        fetchMessages: (convoId) => `/api/conversations/${convoId}/messages`,
        sendMessage: '/api/messages',
        searchUsers: (phone) => `/api/users/search?phone=${phone}`,
        createConversation: '/api/conversations'
    };

    // --- Helper: Get JWT Token ---
    const getAuthHeaders = () => {
        const token = localStorage.getItem('Authorization');
        if (!token) {
            throw new Error('No JWT token found. Please log in.');
        }
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        };
    };

    // --- Initialization ---
    const init = async () => {
        try {
            console.log("Fetching current user...");
            await fetchCurrentUser();
            console.log("Fetching conversations...");
            await fetchConversations();
            console.log("Setting up WebSocket...");
            stompClient = setupWebSocket();
            setupEventListeners();

            messageInput.addEventListener('input', () => {
                messageInput.style.height = 'auto';
                messageInput.style.height = `${messageInput.scrollHeight}px`;
            });
        } catch (error) {
            console.error('Initialization error:', error);
            alert('Failed to initialize chat. Please try logging in again.');
            logout();
        }
    };

    // --- Event Listeners Setup ---
    const setupEventListeners = () => {
        chatList.addEventListener('click', async (e) => {
            const chatItem = e.target.closest('.chat-item');
            if (chatItem) {
                const conversationId = parseInt(chatItem.dataset.conversationId);
                await openConversation(conversationId);
                if (window.innerWidth <= 992) {
                    sidebar.classList.remove('active');
                }
            }
        });

        sendBtn.addEventListener('click', sendMessage);
        messageInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });

        newChatBtn.addEventListener('click', () => newChatModal.show());
        searchUsersInput.addEventListener('input', handleUserSearch);

        searchResults.addEventListener('click', async (e) => {
            const userResult = e.target.closest('.user-result');
            if (userResult) {
                const userId = parseInt(userResult.dataset.userId);
                await startNewConversation(userId);
                newChatModal.hide();
            }
        });

        sidebarToggleBtn.addEventListener('click', () => sidebar.classList.toggle('active'));
    };

    // --- Core Functions ---

    const fetchCurrentUser = async () => {
        try {
            console.log('API Endpoint:', API.fetchCurrentUser);
            console.log('Headers:', getAuthHeaders());
            const response = await fetch(API.fetchCurrentUser, {
                headers: getAuthHeaders()
            });
            console.log('Response:', response);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const user = await response.json();
            currentUserId = user.id; // Use phone as userId
            document.getElementById('current-user-name').textContent = user.fullName;
        } catch (error) {
            console.error('Error fetching current user:', error);
            document.getElementById('current-user-name').textContent = 'Error';
            throw error;
        }
    };

    const fetchConversations = async () => {
        try {
            const response = await fetch(API.fetchConversations, {
                headers: getAuthHeaders()
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            conversations = await response.json();
            renderConversations(conversations);
            subscribeToNotifications();
        } catch (error) {
            console.error('Error fetching conversations:', error);
            chatList.innerHTML = '<p class="text-center text-danger p-3">Could not load chats.</p>';
        }
    };

    const setupWebSocket = () => {
        const token = localStorage.getItem('Authorization');
        if (!token) {
            console.error('No JWT token found. Cannot establish WebSocket connection.');
            alert('Please log in to continue.');
            logout();
            return null;
        }

        const cleanToken = token.replace('Bearer ', '');
        const socket = new SockJS(`/chat?token=${encodeURIComponent(cleanToken)}`);

        const client = new StompJs.Client({
            webSocketFactory: () => socket,
            connectHeaders: {
                Authorization: `Bearer ${cleanToken}`
            },
            debug: (str) => console.log('STOMP: ' + str),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000
        });

        client.onConnect = (frame) => {
            console.log('STOMP connection established:', frame);
            if (activeConversationId) {
                subscribeToConversation(activeConversationId);
            }
            subscribeToNotifications(); // Re-subscribe on connect
            if (messagesList.innerHTML.includes('Disconnected')) {
                messagesList.innerHTML += '<p class="text-center text-success">Reconnected!</p>';
                messagesList.scrollTop = messagesList.scrollHeight;
            }
        };

        client.onWebSocketError = (error) => {
            console.error('WebSocket error:', error);
            messagesList.innerHTML += '<p class="text-center text-danger">WebSocket connection failed. Retrying...</p>';
            messagesList.scrollTop = messagesList.scrollHeight;
        };

        client.onStompError = (frame) => {
            console.error('STOMP error:', frame);
            if (frame.headers['message'].includes('Unauthorized')) {
                alert('Authentication failed. Please log in again.');
                logout();
            } else {
                messagesList.innerHTML += '<p class="text-center text-danger">STOMP error: ' + frame.headers['message'] + '</p>';
            }
        };

        client.onWebSocketClose = (event) => {
            console.log('WebSocket closed:', event);
            messagesList.innerHTML += '<p class="text-center text-muted">Disconnected. Attempting to reconnect...</p>';
            messagesList.scrollTop = messagesList.scrollHeight;
        };

        client.activate();
        return client;
    };

    const sendMessage = () => {
        const text = messageInput.value.trim();
        if (!text || !activeConversationId || !stompClient || !stompClient.connected) {
            console.warn('Cannot send message:', { text, activeConversationId, stompClientConnected: stompClient?.connected });
            messagesList.innerHTML += '<p class="text-center text-danger">Not connected to server. Please try again.</p>';
            messagesList.scrollTop = messagesList.scrollHeight;
            return;
        }

        const tempId = `temp-${Date.now()}`;
        const messageData = {
            conversationId: activeConversationId,
            text: text
        };
        const newMessage = { ...messageData, id: tempId, senderId: currentUserId, timestamp: new Date().toISOString(), read: false };
        //appendMessage(newMessage);
        messageInput.value = '';
        messageInput.style.height = 'auto';

        try {
            stompClient.publish({
                destination: '/app/sendMessage',
                body: JSON.stringify(messageData)
            });
        } catch (error) {
            console.error('Error sending message:', error);
            const tempMessageElement = messagesList.querySelector(`[data-message-id="${tempId}"]`);
            if (tempMessageElement) {
                tempMessageElement.classList.add('error');
                tempMessageElement.innerHTML += '<span class="text-danger">Failed to send</span>';
            }
        }
    };

    const subscribeToConversation = (conversationId) => {
        if (stompClient && stompClient.connected) {
            if (currentSubscription) {
                currentSubscription.unsubscribe();
                currentSubscription = null;
            }

            currentSubscription = stompClient.subscribe(`/topic/messages-${conversationId}`, (message) => {
                const data = JSON.parse(message.body);
                console.log('Received message:', data); // Debug log

                if (data.senderId.toString() === currentUserId.toString()) {
                    // Find temporary message by text and conversationId
                    const tempMessageElement = Array.from(messagesList.querySelectorAll('[data-message-id^="temp-"]'))
                        .find(el => el.querySelector('.message-text').textContent === data.text && el.dataset.conversationId == data.conversationId);

                    if (tempMessageElement) {
                        // Update temporary message with server data
                        tempMessageElement.dataset.messageId = data.id;
                        tempMessageElement.querySelector('.message-text').textContent = data.text;
                        tempMessageElement.querySelector('.message-time').textContent = formatTimestamp(data.timestamp, true);
                        if (data.read) {
                            tempMessageElement.querySelector('.read-status')?.remove();
                            tempMessageElement.innerHTML += '<span class="read-status">Seen</span>';
                        }
                        return; // Prevent appending
                    }
                }
                // Append non-sender messages or sender messages without a temp match
                appendMessage(data);
            });
            console.log(`Subscribed to /topic/messages-${conversationId}`);
            stompClient.publish({
                destination: '/app/markRead',
                body: JSON.stringify({ chatId: conversationId })
            });
        } else {
            console.warn(`Cannot subscribe to /topic/messages-${conversationId}: STOMP client not connected`);
            setTimeout(() => {
                if (stompClient && !stompClient.connected) {
                    stompClient.activate();
                }
            }, 5000);
        }
    };

    const unsubscribeFromConversation = (conversationId) => {
        if (currentSubscription) {
            currentSubscription.unsubscribe();
            currentSubscription = null;
            console.log(`Unsubscribed from /topic/messages-${conversationId}`);
        }
    };

    const subscribeToNotifications = () => {
        if (!stompClient || !stompClient.connected) {
            console.warn('WebSocket not connected, delaying notification subscriptions for user:', currentUserId);
            return;
        }

        console.log('Subscribing to notifications for user:', currentUserId);

        // Unsubscribe from existing subscriptions to avoid duplicates
        if (window.notificationSubscriptions) {
            window.notificationSubscriptions.forEach(sub => sub.unsubscribe());
        }
        window.notificationSubscriptions = [];

        // Notification subscription
        const notificationSub = stompClient.subscribe(`/queue/notifications-${currentUserId}`, (message) => {
            try {
                console.log('Notification received:', message.body);
                const { chatId, unreadCount, latestMessage, timestamp } = JSON.parse(message.body);
                updateConversationItem(chatId, unreadCount, latestMessage, timestamp);
            } catch (error) {
                console.error('Error parsing notification:', error, message.body);
            }
        });
        window.notificationSubscriptions.push(notificationSub);

        // Read subscription
        const readSub = stompClient.subscribe(`/queue/read-${currentUserId}`, (message) => {
            try {
                console.log('Read notification received:', message.body);
                const { chatId, unreadCount } = JSON.parse(message.body);
                if (activeConversationId === chatId) fetchMessages(chatId);
                updateConversationItem(chatId, unreadCount, null, null);
            } catch (error) {
                console.error('Error parsing read notification:', error, message.body);
            }
        });
        window.notificationSubscriptions.push(readSub);

        // Status updates subscription
        const statusSub = stompClient.subscribe(`/topic/status-updates`, (message) => {
            try {
                console.log('Status update received:', message.body);
                const { userId, status } = JSON.parse(message.body);
                updateUserStatus(userId, status);
            } catch (error) {
                console.error('Error parsing status update:', error, message.body);
            }
        });
        window.notificationSubscriptions.push(statusSub);

        // Group-related subscriptions
        conversations.forEach(convo => {
            if (isGroupChat(convo.id)) {
                subscribeToGroupStatus(convo.id);
                subscribeToTypingIndicator(convo.id);
                setupTypingIndicator(convo.id);
            }
        });
    };

    const openConversation = async (conversationId) => {
        activeConversationId = conversationId;
        const conversation = conversations.find(c => c.id === conversationId);

        document.querySelectorAll('.chat-item').forEach(item => {
            item.classList.toggle('active', parseInt(item.dataset.conversationId) === conversationId);
        });
        welcomeScreen.style.display = 'none';
        chatWindow.style.display = 'flex';
        messagesList.innerHTML = '<div class="loading-messages"><div class="spinner"></div><p>Loading messages...</p></div>';

        document.getElementById('chat-user-name').textContent = conversation.name;
        document.getElementById('chat-status').textContent = conversation.status || 'Unknown';
        document.getElementById('chat-header-avatar').style.display = 'flex';

        try {
            const response = await fetch(API.fetchMessages(conversationId), {
                headers: getAuthHeaders()
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const messages = await response.json();
            renderMessages(messages);
            subscribeToConversation(conversationId);
        } catch (error) {
            console.error('Error fetching messages:', error);
            messagesList.innerHTML = '<p class="text-center text-danger p-3">Could not load messages.</p>';
        }
    };

    const handleUserSearch = async (e) => {
        const phone = e.target.value.trim();
        if (phone.length < 3) {
            searchResults.innerHTML = '<div class="no-results">Enter at least 3 digits to search.</div>';
            return;
        }

        searchResults.innerHTML = '<div class="spinner-border spinner-border-sm" role="status"><span class="visually-hidden">Loading...</span></div>';

        try {
            const response = await fetch(API.searchUsers(phone), {
                headers: getAuthHeaders()
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const users = await response.json();
            renderSearchResults(users);
        } catch (error) {
            console.error('Error searching users:', error);
            searchResults.innerHTML = '<div class="no-results text-danger">Error searching.</div>';
        }
    };

    const startNewConversation = async (userId) => {
        try {
            const response = await fetch(API.createConversation, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({ userId })
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const newConversation = await response.json();
            conversations.push(newConversation);
            renderConversations(conversations);
            await openConversation(newConversation.id);
        } catch (error) {
            console.error('Error creating conversation:', error);
            searchResults.innerHTML = '<div class="no-results text-danger">Failed to start conversation.</div>';
        }
    };

    const renderConversations = (convos) => {
        if (convos.length === 0) {
            chatList.innerHTML = '<p class="text-center text-muted p-3">No conversations yet.</p>';
            return;
        }
        chatList.innerHTML = convos.map(convo => `
            <div class="chat-item" data-conversation-id="${convo.id}">
                <div class="chat-avatar">
                    <i class="fas fa-user"></i>
                </div>
                <div class="chat-content">
                    <div class="chat-name">${convo.name}</div>
                    <div class="chat-last-message conversation-preview">${convo.lastMessage || ''}</div>
                </div>
                <div class="chat-meta">
                    <div class="chat-time conversation-time">${formatTimestamp(convo.timestamp)}</div>
                    <div class="chat-status">${convo.status || 'Unknown'}</div>
                    <div class="unread-badge">${convo.unreadCount > 0 ? convo.unreadCount : ''}</div>
                </div>
            </div>
        `).join('');
    };

    const renderMessages = (messages) => {
        messagesList.innerHTML = messages.map(createMessageHTML).join('');
        messagesList.scrollTop = messagesList.scrollHeight;
    };

    const appendMessage = (message) => {
        const messageHTML = createMessageHTML(message);
        messagesList.innerHTML += messageHTML;
        messagesList.scrollTop = messagesList.scrollHeight;
    };

    const createMessageHTML = (message) => {
        console.log("here : "+ message)
        const isSent = message.senderId.toString() == currentUserId;
        const readStatus = isSent && message.read ? '<span class="read-status">Seen</span>' : '';
        return `
            <div class="message ${isSent ? 'sent' : 'received'}" data-message-id="${message.id}">
                <div class="message-content">
                    <p class="message-text">${message.text || message.content || ''}</p>
                    <span class="message-time">${formatTimestamp(message.timestamp || message.createdAt, true)}</span>
                    ${readStatus}
                </div>
            </div>
        `;
    };

    const renderSearchResults = (users) => {
        if (users.length === 0) {
            searchResults.innerHTML = '<div class="no-results">No users found.</div>';
            return;
        }
        searchResults.innerHTML = users.map(user => `
            <div class="user-result" data-user-id="${user.id}">
                <div class="chat-avatar"><i class="fas fa-user"></i></div>
                <div class="user-info">
                    <div class="chat-name">${user.name}</div>
                    <div class="chat-last-message">${user.phone}</div>
                </div>
            </div>
        `).join('');
    };

    const updateConversationItem = (chatId, unreadCount, latestMessage, timestamp) => {
        const conversationItem = document.querySelector(`[data-conversation-id="${chatId}"]`);
        if (conversationItem) {
            if (latestMessage) {
                conversationItem.querySelector('.conversation-preview').textContent = latestMessage;
            }
            if (timestamp) {
                conversationItem.querySelector('.conversation-time').textContent = formatTimestamp(timestamp, true);
            }
            const badge = conversationItem.querySelector('.unread-badge');
            badge.textContent = unreadCount > 0 ? unreadCount : '';
            badge.style.display = unreadCount > 0 ? 'block' : 'none';
        }
    };

    const updateUserStatus = (userId, status) => {
        conversations.forEach(convo => {
            if (convo.name === userId) { // Adjust based on how userId maps to convo.name
                const chatItem = document.querySelector(`[data-conversation-id="${convo.id}"] .chat-status`);
                if (chatItem) {
                    chatItem.textContent = status;
                    chatItem.style.color = status === 'active' ? '#00cc00' : '#666';
                }
            }
        });
    };

    const subscribeToGroupStatus = (chatId) => {
        if (stompClient && stompClient.connected) {
            stompClient.subscribe(`/topic/status-${chatId}`, (message) => {
                const { activeCount } = JSON.parse(message.body);
                updateGroupStatus(chatId, activeCount);
            });
        }
    };

    const updateGroupStatus = (chatId, activeCount) => {
        const statusElement = document.querySelector(`[data-conversation-id="${chatId}"] .group-status`);
        if (statusElement) {
            statusElement.textContent = activeCount > 0 ? `${activeCount} members active now` : 'No members active';
            statusElement.style.display = activeCount > 0 ? 'block' : 'none';
        }
    };

    const subscribeToTypingIndicator = (chatId) => {
        if (stompClient && stompClient.connected) {
            stompClient.subscribe(`/topic/typing-${chatId}`, (message) => {
                const { typingMessage } = JSON.parse(message.body);
                updateTypingIndicator(chatId, typingMessage);
            });
        }
    };

    const updateTypingIndicator = (chatId, typingMessage) => {
        const indicator = document.querySelector(`[data-conversation-id="${chatId}"] .typing-indicator`);
        if (indicator) {
            indicator.textContent = typingMessage;
            indicator.style.display = typingMessage ? 'block' : 'none';
        }
    };

    const setupTypingIndicator = (chatId) => {
        let typingTimer;
        const TYPING_DELAY = 2000;
        messageInput.addEventListener('input', () => {
            clearTimeout(typingTimer);
            if (stompClient && stompClient.connected && isGroupChat(chatId)) {
                stompClient.publish({
                    destination: '/app/typing',
                    body: JSON.stringify({ groupId: chatId, userId: currentUserId })
                });
            }
            typingTimer = setTimeout(() => {
                if (stompClient && stompClient.connected && isGroupChat(chatId)) {
                    stompClient.publish({
                        destination: '/app/typing-stop',
                        body: JSON.stringify({ groupId: chatId, userId: currentUserId })
                    });
                }
            }, TYPING_DELAY);
        });
    };

    const isGroupChat = (chatId) => {
        return conversations.find(c => c.id === chatId)?.chatType === 'group';
    };

    const pingServer = () => {
        if (stompClient && stompClient.connected) {
            stompClient.publish({
                destination: '/app/ping',
                body: JSON.stringify({ userId: currentUserId })
            });
        }
    };
    setInterval(pingServer, 30000);

    const formatTimestamp = (isoString, timeOnly = false) => {
        if (!isoString) return '';
        const date = new Date(isoString);
        if (timeOnly) {
            return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        }
        const now = new Date();
        const diff = (now - date) / (1000 * 60 * 60);
        if (diff < 24) {
            return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } else if (diff < 24 * 7) {
            return date.toLocaleDateString([], { weekday: 'short' });
        } else {
            return date.toLocaleDateString();
        }
    };

    const logout = () => {
        if (stompClient && stompClient.connected) {
            stompClient.deactivate();
        }
        localStorage.removeItem('Authorization');
        window.location.href = 'index.html';
    };

    init();
});