/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The dialog containing a list of all chat rooms of the user and
 * also interface for create a new chat room, join a chat room, search all
 * chat rooms, etc.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Damian Minkov
 */
public class ChatRoomTableDialog
    extends SIPCommDialog
    implements  ActionListener
{
    /**
     * The global/shared <code>ChatRoomTableDialog</code> currently showing.
     */
    private static ChatRoomTableDialog chatRoomTableDialog;

    /**
     * A JComboBox which will allow to select an account for joining a room.
     */
    private JComboBox providersCombo;

    /**
     * An editable JComboBox which will allow to set a room name, and gives
     * suggestions regarding to its content.
     */
    private JComboBox roomsCombo = null;

    /**
     * Rooms of the currently selected provider.
     */
    private List<String> serverRooms = null;

    /**
     * The add chat room button.
     */
    private JButton addButton = new JButton("+");

    /**
     * The remove chat room button.
     */
    private JButton removeButton = new JButton("-");

    /**
     * The ok button.
     */
    private JButton okButton = new JButton(GuiActivator.getResources()
            .getI18NString("service.gui.OK"));

    /**
     * The cancel button.
     */
    private JButton cancelButton = new JButton(GuiActivator.getResources()
            .getI18NString("service.gui.CANCEL"));

    /**
     * The editor for the chat room name.
     */
    private JTextField editor = null;

    /**
     * The available chat rooms list.
     */
    private ChatRoomTableUI chatRoomsTableUI = null;

    /**
     * Shows a <code>ChatRoomTableDialog</code> creating it first if necessary.
     * The shown instance is shared in order to prevent displaying multiple
     * instances of one and the same <code>ChatRoomTableDialog</code>.
     */
    public static void showChatRoomTableDialog()
    {
        if (chatRoomTableDialog == null)
        {
            chatRoomTableDialog
                = new ChatRoomTableDialog(
                        GuiActivator.getUIService().getMainFrame());
            chatRoomTableDialog.setPreferredSize(new Dimension(500, 400));

            /*
             * When the global/shared ChatRoomTableDialog closes, don't keep a
             * reference to it and let it be garbage-collected.
             */
            chatRoomTableDialog.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosed(WindowEvent e)
                {
                    if (chatRoomTableDialog == e.getWindow())
                        chatRoomTableDialog = null;
                }
            });
        }
        chatRoomTableDialog.setVisible(true);
    }

    /**
     * Creates an instance of <tt>MyChatRoomsDialog</tt> by specifying the
     * parent window.
     * 
     * @param parentWindow the parent window of this dialog
     */
    public ChatRoomTableDialog(MainFrame parentWindow)
    {
        super(parentWindow);

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.init();
    }

    /**
     * Initializes this chat room dialog.
     */
    private void init()
    {
        this.getContentPane().setLayout(new BorderLayout(5,5));

        JPanel northPanel = new TransparentPanel(new BorderLayout(5, 5));
        northPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));

        JPanel labels = new TransparentPanel(new GridLayout(3, 2, 5, 5));

        final JLabel jl_searchState = new JLabel(GuiActivator.getResources()
            .getI18NString("service.gui.LOADING_ROOMS"),
                JLabel.LEFT);
        jl_searchState.setBorder(BorderFactory.createEmptyBorder(3, 7, 0, 0));
        jl_searchState.setFont(
           jl_searchState.getFont().deriveFont(Font.ITALIC, 11));
        jl_searchState.setForeground(Color.DARK_GRAY);
        jl_searchState.setVisible(false);
        
        labels.add(new JLabel(GuiActivator.getResources()
            .getI18NString("service.gui.ACCOUNT")));
        labels.add(new JLabel(GuiActivator.getResources()
            .getI18NString("service.gui.CHAT_ROOM_NAME")));
        labels.add(jl_searchState);

        JPanel valuesPanel = new TransparentPanel(new GridLayout(3, 2, 5, 5));
        providersCombo = createProvidersCombobox();

        roomsCombo = new JComboBox();
        roomsCombo.setEditable(true);
        roomsCombo.setPreferredSize(providersCombo.getPreferredSize());
        editor = ((JTextField)roomsCombo.getEditor().getEditorComponent());
        // when enter is typed in the editor we query for available
        // room names
        editor.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                handleChange((JTextField)e.getSource());
            }
        });

        // when provider is changed we load providers rooms list
        // so we can show them in the combobox below
        providersCombo.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e)
            {
                if(e.getStateChange() == ItemEvent.SELECTED)

                new Thread()
                {
                    @Override
                    public void run()
                    {
                        okButton.setEnabled(false);
                        roomsCombo.setEnabled(false);
                        jl_searchState.setVisible(true);
                        loadProviderRooms();
                        jl_searchState.setVisible(false);
                        roomsCombo.setEnabled(true);
                        okButton.setEnabled(true);
                    }
                }.start();
            }
        });

        JLabel jl_indication = new JLabel(GuiActivator.getResources()
            .getI18NString("service.gui.PRESS_ENTER_FOR_SUGGESTIONS"),
            SwingConstants.RIGHT);
        jl_indication.setFont(
           jl_indication.getFont().deriveFont(Font.ITALIC, 11));
        jl_indication.setForeground(Color.DARK_GRAY);

        valuesPanel.add(providersCombo);
        valuesPanel.add(roomsCombo);
        valuesPanel.add(jl_indication);

        northPanel.add(labels, BorderLayout.WEST);
        northPanel.add(valuesPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new TransparentPanel(new BorderLayout(5, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));
        JPanel westButtonPanel = new TransparentPanel();
        JPanel eastButtonPanel = new TransparentPanel();

        addButton.addActionListener(this);
        removeButton.addActionListener(this);
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        westButtonPanel.add(addButton);
        westButtonPanel.add(removeButton);
        eastButtonPanel.add(cancelButton);
        eastButtonPanel.add(okButton);

        buttonPanel.add(westButtonPanel, BorderLayout.WEST);
        buttonPanel.add(eastButtonPanel, BorderLayout.EAST);

        chatRoomsTableUI = new ChatRoomTableUI(this);
        chatRoomsTableUI.setOpaque(false);
        chatRoomsTableUI.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        this.getContentPane().add(northPanel, BorderLayout.NORTH);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        this.getContentPane().add(chatRoomsTableUI, BorderLayout.CENTER);

        loadProviderRooms();

        // when we are typing we clear any selection in the available and saved
        // rooms
        editor.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e)
            {
                chatRoomsTableUI.clearSelection();
            }

            public void keyPressed(KeyEvent e)
            {}

            public void keyReleased(KeyEvent e)
            {}
        });
        // when we select a room from the available ones we clear anyting
        // typed for the room name and set the room we selected
        chatRoomsTableUI.addSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e)
            {
                if(!e.getValueIsAdjusting())
                {
                    ChatRoomWrapper room = chatRoomsTableUI.getSelectedRoom();

                    if(room != null)
                    {
                        editor.setText(room.getChatRoomName());
                        providersCombo.setSelectedItem(room.getParentProvider());
                    }
                }                
            }
        });
    }

    /**
     * Creates the providers combobox and filling its content.
     * @return 
     */
    private JComboBox createProvidersCombobox()
    {
        Iterator<ChatRoomProviderWrapper> providers =
            GuiActivator.getUIService().getConferenceChatManager()
                .getChatRoomList().getChatRoomProviders();

        JComboBox chatRoomProvidersCombobox = new JComboBox();

        while(providers.hasNext())
        {
            chatRoomProvidersCombobox.addItem(providers.next());
        }
        
        chatRoomProvidersCombobox.setRenderer(new ChatRoomProviderRenderer());

        return chatRoomProvidersCombobox;
    }

    /**
     * Handles <tt>ActionEvent</tt>s triggered by a button click.
     * @param e the action event.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton sourceButton = (JButton) e.getSource();
        if(sourceButton.equals(addButton))
        {
            String chatRoomName = editor.getText();

            if(serverRooms == null || !serverRooms.contains(chatRoomName))
            {
                GuiActivator.getUIService().getConferenceChatManager()
                    .createChatRoom(
                        chatRoomName,
                        getSelectedProvider().getProtocolProvider(),
                        new ArrayList<String>(),
                        "",
                        false,
                        true);
            }
        }
        else if(sourceButton.equals(removeButton))
        {
            chatRoomsTableUI.removeSelectedRoom();
        }
        else if(sourceButton.equals(okButton))
        {
            ChatRoomWrapper selectedRoom = chatRoomsTableUI.getSelectedRoom();

            if(selectedRoom == null)
            {
                if(editor.getText() != null
                        && editor.getText().trim().length() > 0)
                {
                    ChatRoomWrapper chatRoomWrapper =
                    GuiActivator.getUIService().getConferenceChatManager()
                        .createChatRoom(
                            editor.getText(),
                            getSelectedProvider().getProtocolProvider(),
                            new ArrayList<String>(),
                            "",
                            true,
                            false);

                    ChatWindowManager chatWindowManager
                        = GuiActivator.getUIService().getChatWindowManager();
                    ChatPanel chatPanel
                        = chatWindowManager.getMultiChat(chatRoomWrapper, true);

                    chatWindowManager.openChat(chatPanel, true);
                }
            }
            else
            {
                if(selectedRoom.getChatRoom() != null)
                {
                    if(!selectedRoom.getChatRoom().isJoined())
                        GuiActivator.getUIService().getConferenceChatManager()
                            .joinChatRoom(selectedRoom);
                    else
                        chatRoomsTableUI.openChatForSelection();
                }
                else 
                {
                    // this is not a server persistent room we must create it
                    // and join
                    ChatRoomWrapper chatRoomWrapper =
                    GuiActivator.getUIService().getConferenceChatManager()
                        .createChatRoom(
                            selectedRoom.getChatRoomName(),
                            getSelectedProvider().getProtocolProvider(),
                            new ArrayList<String>(),
                            "",
                            true,
                            true);
                    ChatWindowManager chatWindowManager
                        = GuiActivator.getUIService().getChatWindowManager();

                    ChatPanel chatPanel
                        = chatWindowManager.getMultiChat(chatRoomWrapper, true);

                    chatWindowManager.openChat(chatPanel, true);
                }
            }

            // in all cases we dispose this dialog
            dispose();
        }
        else if(sourceButton.equals(cancelButton))
        {
            dispose();
        }
    }

    @Override
    protected void close(boolean isEscaped)
    {
        chatRoomTableDialog = null;
    }

    /**
     * Performs changes in the room name combo box when its editor content has
     * changed.
     * @param editor 
     */
    public void handleChange(final JTextField editor)
    {
        final String match = editor.getText();

        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable(){
                public void run()
                {
                    handleChange(editor);
                }
            });

            return;
        }

        roomsCombo.removeAllItems();

        for(String room : getChatRoomList(match))
            roomsCombo.addItem(room);

        editor.setText(match);
        roomsCombo.showPopup();
    }

    /**
     * Updates the chat rooms list when a key change is performed in the search
     * field. The new chat rooms list will contain all the chat rooms whose name
     * start with search fields text value.
     * @param match search for.
     * @return the found rooms.
     */
    public Vector<String> getChatRoomList(String match)
    {
        Vector<String> rooms = new Vector<String>();

        if(serverRooms != null)
            for(String room : serverRooms)
                if(room.startsWith(match))
                   rooms.add(room);

        Collections.sort(rooms);
        return rooms;
    }

    /**
     * Loads the rooms hosted on the selected provider.
     */
    public void loadProviderRooms()
    {
        serverRooms = GuiActivator.getUIService().getConferenceChatManager()
            .getExistingChatRooms(getSelectedProvider());
    }

    /**
     * Returns the selected provider in the providers combo box.
     *
     * @return the selected provider
     */
    public ChatRoomProviderWrapper getSelectedProvider()
    {
        return (ChatRoomProviderWrapper)providersCombo.getSelectedItem();
    }

    /**
     * Cell renderer for the providers combo box: displays the protocol name
     * with its associated icon.
     */
    class ChatRoomProviderRenderer
        extends JLabel
        implements ListCellRenderer
    {
        /**
         * The renderer.
         */
        public ChatRoomProviderRenderer()
        {
            setOpaque(true);
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(CENTER);
            this.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }

        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {
            ChatRoomProviderWrapper provider = (ChatRoomProviderWrapper)value;

            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setText(provider.getProtocolProvider()
                .getAccountID().getAccountAddress());
            
            setIcon(new ImageIcon(provider.getProtocolProvider()
                    .getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16)));

            return this;
        }
    }
}