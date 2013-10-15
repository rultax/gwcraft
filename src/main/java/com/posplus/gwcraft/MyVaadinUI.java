package com.posplus.gwcraft;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Container;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.data.util.sqlcontainer.connection.JDBCConnectionPool;
import com.vaadin.data.util.sqlcontainer.connection.SimpleJDBCConnectionPool;
import com.vaadin.data.util.sqlcontainer.query.TableQuery;
import com.vaadin.data.util.sqlcontainer.query.generator.MSSQLGenerator;
import com.vaadin.event.FieldEvents;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import java.sql.SQLException;

@Theme("gwcraft")
@SuppressWarnings("serial")
public class MyVaadinUI extends UI
{
    private Table itemList = new Table();
    private TextField searchField = new TextField();
    private Button addNewItemButton = new Button("New");
    private Button removeItemButton = new Button("Remove this contact");
    private FormLayout editorLayout = new FormLayout();
    private FieldGroup editorFields = new FieldGroup();
    
    SQLContainer itemContainer = createItemData();
    
    private static final String ID = "ItemId";
    private static final String ITEMNAME = "ItemName";
    private static final String[] fieldNames = new String[] {
        ID, ITEMNAME
    };
    
    
    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = MyVaadinUI.class, widgetset = "com.posplus.gwcraft.AppWidgetSet")
    public static class Servlet extends VaadinServlet {
    }

    @Override
    protected void init(VaadinRequest request) {
        initLayout();
        initItemList();
        initEditor();
        initSearch();
        initAddRemoveButtons();
    }
    
    private void initLayout(){
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        setContent(splitPanel);
        
        VerticalLayout leftLayout = new VerticalLayout();
        splitPanel.addComponent(leftLayout);
        splitPanel.addComponent(editorLayout);
        leftLayout.addComponent(itemList);
        HorizontalLayout bottomLeftLayout = new HorizontalLayout();
        leftLayout.addComponent(bottomLeftLayout);
        bottomLeftLayout.addComponent(searchField);
        bottomLeftLayout.addComponent(addNewItemButton);
        
        leftLayout.setSizeFull();
        
        leftLayout.setExpandRatio(itemList, 1);
        itemList.setSizeFull();
        
        bottomLeftLayout.setWidth("100%");
        searchField.setWidth("100%");
        bottomLeftLayout.setExpandRatio(searchField, 1);
    }
    
    private void initEditor(){
        for(String fieldName : fieldNames){
            TextField field = new TextField(fieldName);
            editorLayout.addComponent(field);
            field.setWidth("100%");
            
            editorFields.bind(field, fieldName);
        }
        
        editorLayout.addComponent(removeItemButton);
        
        editorFields.setBuffered(false);
    }
    
    private void initSearch(){
        searchField.setInputPrompt("Search items");
        
        searchField.setTextChangeEventMode(AbstractTextField.TextChangeEventMode.LAZY);
        
        searchField.addTextChangeListener(new FieldEvents.TextChangeListener() {

            @Override
            public void textChange(FieldEvents.TextChangeEvent event) {
                itemContainer.removeAllContainerFilters();
                itemContainer.addContainerFilter(new ItemFilter(event.getText()));
            }
        });
    }
    
    private class ItemFilter implements Filter{
        private String needle;
        
        public ItemFilter(String needle){
            this.needle = needle.toLowerCase();
        }
        
        @Override
        public boolean passesFilter(Object itemId, Item item) {
            String haystack = ("" + item.getItemProperty(ITEMNAME).getValue()).toLowerCase();
            return haystack.contains(needle);
        }
        
        @Override
        public boolean appliesToProperty(Object id){
            return true;
        }
    }
    
    private void initAddRemoveButtons(){
        addNewItemButton.addClickListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                itemContainer.removeAllContainerFilters();
                Object itemId = itemContainer.addItemAt(0);
                itemList.getContainerProperty(itemId, ITEMNAME).setValue("New Item");
                itemList.select(itemId);
            }
        });
        
        removeItemButton.addClickListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                Object itemId = itemList.getValue();
                itemList.removeItem(itemId);
            }
        });
    }
    
    private void initItemList(){
        itemList.setContainerDataSource(itemContainer);
        itemList.setVisibleColumns(ITEMNAME);
        itemList.setSelectable(true);
        itemList.setImmediate(true);
        
        itemList.addValueChangeListener(new Property.ValueChangeListener() {

            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                Object itemId = itemList.getValue();
                
                if(itemId != null)
                    editorFields.setItemDataSource(itemList.getItem(itemId));
                
                editorLayout.setVisible(itemId != null);
            }
        });
    }

    private static SQLContainer createItemData() {
        try {
            JDBCConnectionPool pool = new SimpleJDBCConnectionPool(
                    "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                    "jdbc:sqlserver://localhost\\Development;Database=GW2",
                    "sa",
                    "PosPlus80101!");
            TableQuery tq = new TableQuery("Items", pool, new MSSQLGenerator() );
            return new SQLContainer(tq);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }    
}
