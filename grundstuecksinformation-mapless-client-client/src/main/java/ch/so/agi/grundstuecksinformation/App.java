package ch.so.agi.grundstuecksinformation;

import static elemental2.dom.DomGlobal.console;
import static elemental2.dom.DomGlobal.fetch;
import static org.jboss.elemento.Elements.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.dominokit.domino.ui.badges.Badge;
import org.dominokit.domino.ui.breadcrumbs.Breadcrumb;
import org.dominokit.domino.ui.datatable.ColumnConfig;
import org.dominokit.domino.ui.datatable.DataTable;
import org.dominokit.domino.ui.datatable.TableConfig;
import org.dominokit.domino.ui.datatable.store.LocalListDataStore;
import org.dominokit.domino.ui.dropdown.DropDownMenu;
import org.dominokit.domino.ui.forms.AbstractSuggestBox.DropDownPositionDown;
import org.dominokit.domino.ui.forms.SuggestBox;
import org.dominokit.domino.ui.forms.SuggestBoxStore;
import org.dominokit.domino.ui.forms.SuggestItem;
import org.dominokit.domino.ui.icons.Icon;
import org.dominokit.domino.ui.icons.Icons;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.style.ColorScheme;
import org.dominokit.domino.ui.themes.Theme;
import org.dominokit.domino.ui.utils.HasSelectionHandler.SelectionHandler;
import org.dominokit.domino.ui.utils.TextNode;
import org.eclipse.jetty.util.log.Log;
import org.gwtproject.safehtml.shared.SafeHtmlUtils;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.NumberFormat;

import elemental2.core.Global;
import elemental2.core.JsArray;
import elemental2.core.JsNumber;
import elemental2.core.JsString;
import elemental2.dom.DomGlobal;
import elemental2.dom.Event;
import elemental2.dom.EventListener;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLInputElement;
import elemental2.dom.Headers;
import elemental2.dom.Location;
import elemental2.dom.RequestInit;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import ol.Extent;
import ol.Feature;
import ol.Map;
import ol.OLFactory;
import ol.format.GeoJson;
import ol.proj.Projection;
import ol.proj.ProjectionOptions;
import proj4.Proj4;

public class App implements EntryPoint {

    // Application settings
    private String myVar;
    //private String SEARCH_SERVICE_URL = "https://geo.so.ch/api/search/v2/?filter=ch.so.agi.av.gebaeudeadressen.gebaeudeeingaenge,ch.so.agi.av.grundstuecke.rechtskraeftig,ch.so.agi.av.grundstuecke.projektierte&searchtext=";    
    private String SEARCH_SERVICE_URL = "https://geo.so.ch/api/search/v2/?filter=ch.so.agi.av.gebaeudeadressen.gebaeudeeingaenge,ch.so.agi.av.grundstuecke.rechtskraeftig&searchtext=";    
    private String DATA_SERVICE_URL = "https://geo.so.ch/api/data/v1/";
    private String OEREB_SERVICE_URL = "https://geo.so.ch/api/oereb/extract/reduced/pdf/";
    private String OEREB_WEB_URL = "https://geo.so.ch/map/?oereb_egrid=";
    //private String AV_SERVICE_URL = "https://av.sogeo.services/extract/pdf/geometry/";
    private String AV_SERVICE_URL = "https://geo.so.ch/api/v1/document/grundstuecksbeschrieb?feature=";
    private String AV_MAP_URL = "https://av.sogeo.services/extract/pdf/map/";

    // Format settings
    private NumberFormat fmtDefault = NumberFormat.getDecimalFormat();
    private NumberFormat fmtPercent = NumberFormat.getFormat("#0.0");

    // Projection
    private static final String EPSG_2056 = "EPSG:2056";
    private static final String EPSG_4326 = "EPSG:4326"; 
    private Projection projection;
    
    private String MAP_DIV_ID = "map";
    private Map map;

    private HTMLElement container;
    private HTMLElement topLevelContent;
    private SuggestBox suggestBox;
    private List<Grundstueck> grundstuecke;
    private LocalListDataStore<Grundstueck> listStore;
    private DataTable<Grundstueck> datasetTable;

	public void onModuleLoad() {
	    init();
	}
	
	public void init() {
	    // Registering EPSG:2056 / LV95 reference frame.
        Proj4.defs(EPSG_2056, "+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=2600000 +y_0=1200000 +ellps=bessel +towgs84=674.374,15.056,405.346,0,0,0,0 +units=m +no_defs");
        ol.proj.Proj4.register(Proj4.get());

        ProjectionOptions projectionOptions = OLFactory.createOptions();
        projectionOptions.setCode(EPSG_2056);
        projectionOptions.setUnits("m");
        projectionOptions.setExtent(new Extent(2420000, 1030000, 2900000, 1350000));
        projection = new Projection(projectionOptions);
        Projection.addProjection(projection);
        
        Theme theme = new Theme(ColorScheme.WHITE);
        theme.apply();
              
        container = div().id("container").element();
        body().add(container);

        Location location = DomGlobal.window.location;
        if (location.pathname.length() > 1) {
            location.pathname += "/"; 
        }

        HTMLElement logoDiv = div().css("logo")
                .add(div()
                        .add(img().attr("src", location.protocol + "//" + location.host + location.pathname + "Logo.png").attr("alt", "Logo Kanton")).element()).element();
        container.appendChild(logoDiv);

        topLevelContent = div().id("top-level-content").element();
        container.appendChild(topLevelContent);

        Breadcrumb breadcrumb = Breadcrumb.create().appendChild(Icons.ALL.home(), " Home ", (evt) -> {
            DomGlobal.window.open("https://geo.so.ch/", "_blank");
        }).appendChild(" Grundstücksinformation ", (evt) -> {
        });
        topLevelContent.appendChild(breadcrumb.element());

        topLevelContent.appendChild(div().css("main-title").textContent("Grundstücksinformation Kanton Solothurn").element());

        String infoString = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy "
                + "<a class='default-link' href='https://geo.so.ch/map' target='_blank'>https://geo.so.ch/map</a> eirmod "
                + "tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et "
                + "justo duo dolores et ea rebum. Stet clita kasd gubergren no sea takimata sanctus est Lorem ipsum dolor sit amet.";

        topLevelContent.appendChild(div().css("info").innerHtml(SafeHtmlUtils.fromTrustedString(infoString)).element());

        SuggestBoxStore dynamicStore = new SuggestBoxStore() {
            @Override
            public void filter(String value, SuggestionsHandler suggestionsHandler) {
                if (value.trim().length() == 0) {
                    return;
                }
                
                RequestInit requestInit = RequestInit.create();
                Headers headers = new Headers();
                headers.append("Content-Type", "application/x-www-form-urlencoded");
                requestInit.setHeaders(headers);
                
                DomGlobal.fetch(SEARCH_SERVICE_URL + value.trim().toLowerCase(), requestInit)
                .then(response -> {
                    if (!response.ok) {
                        return null;
                    }
                    return response.text();
                })
                .then(json -> {
                    List<SuggestItem<SearchResult>> featureResults = new ArrayList<SuggestItem<SearchResult>>();
                    List<SuggestItem<SearchResult>> suggestItems = new ArrayList<>();
                    JsPropertyMap<?> parsed = Js.cast(Global.JSON.parse(json));
                    JsArray<?> results = Js.cast(parsed.get("results"));
                    for (int i = 0; i < results.length; i++) {
                        JsPropertyMap<?> resultObj = Js.cast(results.getAt(i));
                                                    
                        if (resultObj.has("feature")) {
                            JsPropertyMap feature = (JsPropertyMap) resultObj.get("feature");
                            String display = ((JsString) feature.get("display")).normalize();
                            String dataproductId = ((JsString) feature.get("dataproduct_id")).normalize();
                            String idFieldName = ((JsString) feature.get("id_field_name")).normalize();
                            int featureId = new Double(((JsNumber) feature.get("feature_id")).valueOf()).intValue();
                            List<Double> bbox = ((JsArray) feature.get("bbox")).asList();
 
                            SearchResult searchResult = new SearchResult();
                            searchResult.setLabel(display);
                            searchResult.setDataproductId(dataproductId);
                            searchResult.setIdFieldName(idFieldName);
                            searchResult.setFeatureId(featureId);
                            searchResult.setBbox(bbox);
                            searchResult.setType("feature");
                            
                            Icon icon;
                            if (dataproductId.contains("gebaeudeadressen")) {
                                icon = Icons.ALL.mail();
                            } else if (dataproductId.contains("grundstueck")) {
                                icon = Icons.ALL.home();
                            } else {
                                icon = Icons.ALL.place();
                            }
                            
                            SuggestItem<SearchResult> suggestItem = SuggestItem.create(searchResult, searchResult.getLabel(), icon);
                            featureResults.add(suggestItem);
                        }
                    }
                    suggestItems.addAll(featureResults);
                    suggestionsHandler.onSuggestionsReady(suggestItems);
                    return null;
                }).catch_(error -> {
                    console.log(error);
                    return null;
                });
            }

            @Override
            public void find(Object searchValue, Consumer handler) {
                if (searchValue == null) {
                    return;
                }
                HTMLInputElement el = (HTMLInputElement) suggestBox.getInputElement().element();
                SearchResult searchResult = (SearchResult) searchValue;
                SuggestItem<SearchResult> suggestItem = SuggestItem.create(searchResult, el.value);
                handler.accept(suggestItem);
            }
        };
        
        suggestBox = SuggestBox.create("Suche: Grundstücke und Adressen", dynamicStore);
        suggestBox.addLeftAddOn(Icons.ALL.search());
        suggestBox.setAutoSelect(false);
        suggestBox.setFocusColor(Color.RED_DARKEN_3);
        suggestBox.setFocusOnClose(false);

        HTMLElement resetIcon = Icons.ALL.close().setId("search-reset-icon").element();
        resetIcon.style.cursor = "pointer";
        resetIcon.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                HTMLInputElement el =(HTMLInputElement) suggestBox.getInputElement().element();
                el.value = "";
                suggestBox.unfocus();
                if (datasetTable != null) {
                    datasetTable.style().setDisplay("none", true);                    
                }
                //listStore = new LocalListDataStore<>();
            }
        });
        suggestBox.addRightAddOn(resetIcon);

        suggestBox.getInputElement().setAttribute("autocomplete", "off");
        suggestBox.getInputElement().setAttribute("spellcheck", "false");
        DropDownMenu suggestionsMenu = suggestBox.getSuggestionsMenu();
        suggestionsMenu.setPosition(new DropDownPositionDown());
        suggestionsMenu.setSearchable(false);
        
        suggestBox.addSelectionHandler(new MySelectionHandler());

        topLevelContent.appendChild(div().id("search-panel").add(div().id("suggestbox-div").add(suggestBox)).element());

        TableConfig<Grundstueck> tableConfig = new TableConfig<>();
        tableConfig
                .addColumn(ColumnConfig.<Grundstueck>create("number", "Nummer").setShowTooltip(false).textAlign("left")
                        .setCellRenderer(cell -> TextNode.of(cell.getTableRow().getRecord().getNumber())))
                .addColumn(ColumnConfig.<Grundstueck>create("display", "Suchobjekt").setShowTooltip(false)
                        .textAlign("left").setCellRenderer(cell -> TextNode.of(cell.getTableRow().getRecord().getDisplay())))
                .addColumn(ColumnConfig.<Grundstueck>create("type", "Grundstücksart").setShowTooltip(false)
                        .textAlign("left").setCellRenderer(cell -> TextNode.of(cell.getTableRow().getRecord().getType())))
                .addColumn(ColumnConfig.<Grundstueck>create("type", "Rechtsstatus").setShowTooltip(false)
                        .textAlign("left").setCellRenderer(cell -> TextNode.of(cell.getTableRow().getRecord().getLawStatus())))
                .addColumn(ColumnConfig.<Grundstueck>create("oereb", "ÖREB-Kataster").setShowTooltip(false)
                        .textAlign("left").setCellRenderer(cell -> {
                            HTMLElement badgesElement = div().element();
                            
                            badgesElement.appendChild(a().css("badge-link")
                                    .attr("href", OEREB_SERVICE_URL + cell.getTableRow().getRecord().getEgrid())
                                    .attr("target", "_blank")
                                    .add(Badge.create("Auszug")
                                            .setBackground(Color.GREY_LIGHTEN_2).style()
                                            .setMarginRight("10px").setMarginTop("5px")
                                            .setMarginBottom("5px").get().element())
                            .element());

                            badgesElement.appendChild(a().css("badge-link")
                                    .attr("href", OEREB_WEB_URL + cell.getTableRow().getRecord().getEgrid())
                                    .attr("target", "_blank")
                                    .add(Badge.create("Web")
                                            .setBackground(Color.GREY_LIGHTEN_2).style()
                                            .setMarginRight("10px").setMarginTop("5px")
                                            .setMarginBottom("5px").get().element())
                            .element());

                            return badgesElement;
                        }))
                .addColumn(ColumnConfig.<Grundstueck>create("grundstuecksbeschrieb", "Grundstücksbeschrieb").setShowTooltip(false)
                        .textAlign("left").setCellRenderer(cell -> {
                            HTMLElement badgesElement = div().element();
                            
                            badgesElement.appendChild(a().css("badge-link")
                                    //.attr("href", AV_SERVICE_URL + cell.getTableRow().getRecord().getEgrid())
                                    .attr("href", AV_SERVICE_URL + cell.getTableRow().getRecord().getId())
                                    .attr("target", "_blank")
                                    .add(Badge.create("Beschrieb")
                                            .setBackground(Color.GREY_LIGHTEN_2).style()
                                            .setMarginRight("10px").setMarginTop("5px")
                                            .setMarginBottom("5px").get().element())
                            .element());

                            badgesElement.appendChild(a().css("badge-link")
                                    .attr("href", AV_MAP_URL + cell.getTableRow().getRecord().getEgrid())
                                    .attr("target", "_blank")
                                    .add(Badge.create("Plan")
                                            .setBackground(Color.GREY_LIGHTEN_2).style()
                                            .setMarginRight("10px").setMarginTop("5px")
                                            .setMarginBottom("5px").get().element())
                            .element());

                            badgesElement.appendChild(a().css("badge-link")
                                    .attr("href", OEREB_WEB_URL + cell.getTableRow().getRecord().getEgrid())
                                    .attr("target", "_blank")
                                    .add(Badge.create("Web")
                                            .setBackground(Color.GREY_LIGHTEN_2).style()
                                            .setMarginRight("10px").setMarginTop("5px")
                                            .setMarginBottom("5px").get().element())
                            .element());


                            return badgesElement;
                        }))
                .addColumn(ColumnConfig.<Grundstueck>create("eigentuemer", "Eigentümberabfrage").setShowTooltip(false)
                        .textAlign("left").setCellRenderer(cell -> {
                            HTMLElement badgesElement = div().element();
                           
                            badgesElement.appendChild(a().css("badge-link")
                                    .attr("href", OEREB_WEB_URL + cell.getTableRow().getRecord().getEgrid())
                                    .attr("target", "_blank")
                                    .add(Badge.create("Web")
                                            .setBackground(Color.GREY_LIGHTEN_2).style()
                                            .setMarginRight("10px").setMarginTop("5px")
                                            .setMarginBottom("5px").get().element())
                            .element());


                            return badgesElement;
                        }));



        listStore = new LocalListDataStore<>();

        datasetTable = new DataTable<>(tableConfig, listStore);
        datasetTable.setId("dataset-table");
        datasetTable.noStripes();
        datasetTable.style().setDisplay("none", true);

        topLevelContent.appendChild(datasetTable.element());        

//        // Add the Openlayers map (element) to the body.
//        HTMLElement mapElement = div().id(MAP_DIV_ID).element();
//        body().add(mapElement);
//        map = MapPresets.getColorMap(MAP_DIV_ID);        
	}
	
    public class MySelectionHandler implements SelectionHandler {
        @Override
        public void onSelection(Object value) {
            grundstuecke = new ArrayList<Grundstueck>();
            
            SuggestItem<SearchResult> item = (SuggestItem<SearchResult>) value;
            SearchResult result = (SearchResult) item.getValue();
            
            RequestInit requestInit = RequestInit.create();
            Headers headers = new Headers();
            headers.append("Content-Type", "application/x-www-form-urlencoded"); // CORS and preflight...
            requestInit.setHeaders(headers);
            
            if (result.getType().equalsIgnoreCase("feature")) {
                String dataproductId = result.getDataproductId();
                String idFieldName = result.getIdFieldName();
                String featureId = String.valueOf(result.getFeatureId());
                
                String requestUrl;
                if (dataproductId.equalsIgnoreCase("ch.so.agi.av.gebaeudeadressen.gebaeudeeingaenge")) {
                    List<Double> bbox = result.getBbox();                 
                    String bboxStr = bbox.get(0).toString()+","+bbox.get(1).toString()+","+bbox.get(2).toString()+","+bbox.get(3).toString();
                    requestUrl = DATA_SERVICE_URL + "ch.so.agi.av.grundstuecke.rechtskraeftig" + "/?bbox="+bboxStr;
                } else {
                    requestUrl = DATA_SERVICE_URL + dataproductId + "/?filter=[[\""+idFieldName+"\",\"=\","+featureId+"]]";
                }
                
                DomGlobal.fetch(requestUrl, requestInit)
                .then(response -> {
                    if (!response.ok) {
                        return null;
                    }
                    return response.text();
                })
                .then(json -> {                    
                    Feature[] features = (new GeoJson()).readFeatures(json); 
                                        
                    for (Feature feature : features) {
                        Grundstueck gs = new Grundstueck();
                        gs.setDisplay(result.getDisplay());

                        String egrid = Js.asString(feature.getProperties().get("egrid"));                        
                        gs.setEgrid(egrid);
                        
                        String number = Js.asString(feature.getProperties().get("nummer"));
                        gs.setNumber(number);
                        
                        String type = Js.asString(feature.getProperties().get("art_txt"));
                        gs.setType(type);
                        
                        gs.setId(feature.getId());
                        
                        if (dataproductId.contains("projektiert")) {
                            gs.setLawStatus("projektiert");
                        } else {
                            gs.setLawStatus("rechtsgültig");
                        }
                        
                        console.log(gs.toString());
                        
                        
                        grundstuecke.add(gs);
                    }           
                    
                    listStore.setData(grundstuecke);
                    datasetTable.load();
                    datasetTable.style().setDisplay("block", true);

                    return null;
                }).catch_(error -> {
                    console.log(error);
                    return null;
                });
                
                
//                // Zoom to feature.
//                List<Double> bbox = result.getBbox();                 
//                Extent extent = new Extent(bbox.get(0), bbox.get(1), bbox.get(2), bbox.get(3));
//                View view = map.getView();
//                double resolution = view.getResolutionForExtent(extent);
//                view.setZoom(Math.floor(view.getZoomForResolution(resolution)) - 1);
//                double x = extent.getLowerLeftX() + extent.getWidth() / 2;
//                double y = extent.getLowerLeftY() + extent.getHeight() / 2;
//                view.setCenter(new Coordinate(x,y));
            }
        }
    }
}