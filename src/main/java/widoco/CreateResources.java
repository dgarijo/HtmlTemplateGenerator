/*
 * Copyright 2012-2013 Ontology Engineering Group, Universidad Polit�cnica de Madrid, Spain
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package widoco;

import diagram.DiagramGeneration;
import diff.CompareOntologies;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import javax.swing.JOptionPane;
import lode.LODEGeneration;
import org.semanticweb.owlapi.formats.N3DocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import widoco.entities.Agent;
import widoco.entities.Ontology;

/**
 * Class that given a path, it creates all the associated resources needed to
 * view the documentation. Also, it builds the structure of the folder
 * @author Daniel Garijo
 */
public class CreateResources {
    
    //to do: analyze if this is the right name for the class. Maybe "generate" is better
    public static void generateDocumentation(String outFolder, Configuration c, File lodeResources) throws Exception{
        String lodeContent;
        String folderOut = outFolder;
        Properties languageFile = new Properties();
        try{
            String resource = "/widoco/"+c.getCurrentLanguage()+".properties";
            languageFile.load(CreateResources.class.getResourceAsStream(resource));
        }catch (Exception e){
            String resource = "/widoco/en.properties";
            try{
                System.out.println("Language file not found for "+c.getCurrentLanguage()+"!! Loading english by default");
                languageFile.load(CreateResources.class.getResourceAsStream(resource));
            }catch(Exception e1){
                System.out.println("Error while reading the language file: "+e1.getMessage());
            }
        }
        lodeContent = LODEGeneration.getLODEhtml(c, lodeResources);    
        LODEParser lode = new LODEParser(lodeContent,c,languageFile);
        if(c.isCreateHTACCESS()){
            File f = new File (folderOut);
            if(!f.exists()){
                f.mkdir();
            }
            createHTACCESSFile(folderOut+File.separator+".htaccess",c);
            //slash ontologies require a special type of redirection
            if(!c.getMainOntology().isHashOntology()){
                folderOut+=File.separator+"doc";
            }
        }
        createFolderStructure(folderOut,c,languageFile);
        if(c.isIncludeAbstract()){
            createAbstractSection(folderOut+File.separator+"sections",c, languageFile);
        }
        if(c.isIncludeIntroduction()){
            createIntroductionSection(folderOut+File.separator+"sections",lode.getNamespaceDeclarations(),c, languageFile);
        }
        if(c.isIncludeOverview()){
            createOverviewSection(folderOut+File.separator+"sections",c, lode.getClassList(),lode.getPropertyList(),lode.getDataPropList(), lode.getAnnotationPropList(),lode.getNamedIndividualList(), languageFile);
        }
        if(c.isIncludeDescription()){
            createDescriptionSection(folderOut+File.separator+"sections",c, languageFile);
        }
        if(c.isIncludeCrossReferenceSection()){
            createCrossReferenceSection(folderOut+File.separator+"sections",lode, c, languageFile);
        }
        if(c.isIncludeReferences()){
            createReferencesSection(folderOut+File.separator+"sections",c, languageFile);
        }
        if(c.isPublishProvenance()){
            createProvenancePage(folderOut+File.separator+"provenance", c, languageFile);
        }
        if(c.isIncludeChangeLog()){
            if(c.getMainOntology().getPreviousVersion()!=null &&!"".equals(c.getMainOntology().getPreviousVersion())){
                createChangeLog(folderOut+File.separator+"sections", c, languageFile);
            }else{
                System.out.println("No previous version provided. No changelog produced!");
            }
        }
        //diagram creation
        DiagramGeneration.generateOntologyDiagram(folderOut, c);
    
        //serialize the model in different serializations.
        OWLOntologyManager om = c.getMainOntology().getOWLAPIOntologyManager();
        OWLOntology o = c.getMainOntology().getOWLAPIModel();
        WidocoUtils.writeModel(om, o, new RDFXMLDocumentFormat(), folderOut+File.separator+"ontology.xml");
        WidocoUtils.writeModel(om, o, new TurtleDocumentFormat(), folderOut+File.separator+"ontology.ttl");
        WidocoUtils.writeModel(om, o, new N3DocumentFormat(), folderOut+File.separator+"ontology.n3");
        
//        HashMap<String,String> s = c.getMainOntology().getSerializations();
//        for(String serialization:s.keySet()){
//            OutputStream out = null;
//            String sValue = s.get(serialization);
//            if(sValue.startsWith("ontology")){
//                try {
//                    out = new FileOutputStream(folderOut+File.separator+sValue);
//                    //c.getMainOntology().getMainModel().write(out,serialization);
//                    c.getMainOntology().getOWLAPIOntologyManager().saveOntology(c.getMainOntology().getOWLAPIModel(), owldf, out);
//                    out.close();
//                } catch (Exception ex) {
//                    System.out.println("Error while writing the model to file "+ex.getMessage());
//                    if(out!=null){
//                        out.close();
//                    }
//                }
//            }
//        }
        if(c.isIncludeIndex()){
            createIndexDocument(folderOut,c, lode, languageFile);
        }
    }
    
    public static void generateSkeleton(String folderOut, Configuration c, Properties l){
//        c.setTitle("Skeleton title");
        c.setIncludeDiagram(false);
        c.setPublishProvenance(false);
        c.setUseW3CStyle(true);
        createFolderStructure(folderOut,c,l);
        createAbstractSection(folderOut+File.separator+"sections",c, l);
        createIntroductionSection(folderOut+File.separator+"sections",null,c,l);
        createDescriptionSection(folderOut+File.separator+"sections",c,l);
        createReferencesSection(folderOut+File.separator+"sections",c,l);
        createIndexDocument(folderOut,c,null, l);
    }
    
    /**
     * Provenance page
     */
    private static void createProvenancePage(String path, Configuration c, Properties lang){
        saveDocument(path+File.separator+"provenance-"+c.getCurrentLanguage()+".html", Constants.getProvenanceHtml(c, lang),c);
        saveDocument(path+File.separator+"provenance-"+c.getCurrentLanguage()+".ttl", Constants.getProvenanceRDF(c),c);
    }
    
    /**
     * Method that creates an htaccess file for content negotiation
     * @param path where to save the file
     * @param c configuration with the information of the current settings
     */
    private static void createHTACCESSFile(String path, Configuration c){
        saveDocument(path,Constants.getHTACCESS(c), c);
    }
    /**
     * Method that creates a 406 page in case the user ir requesting an unsupported serialization
     * @param path where to save the file
     * @param c configuration with the information of the current settings
     * @lang patameter with the language properties document with the translations
     */
    private static void create406Page(String path, Configuration c, Properties lang) {
        saveDocument(path,Constants.get406(c,lang), c);
    }
    
    /**
     * Method that creates the change log for the ontology, automatically.
     * @param path
     * @param c
     * @param lang 
     */
    private static void createChangeLog(String path, Configuration c, Properties lang){
        try{
            System.out.println("Attempting to generate an automated changelog\nDownloading old ontology "+c.getMainOntology().getPreviousVersion());
            String oldVersionPath = c.getTmpFile().getAbsolutePath()+File.separator+"OLDOntology";
            WidocoUtils.downloadOntology(c.getMainOntology().getPreviousVersion(), oldVersionPath);
            CompareOntologies comparison = new CompareOntologies(oldVersionPath, c);
            saveDocument(path+File.separator+"changelog-"+c.getCurrentLanguage()+".html", Constants.getChangeLogSection(c, comparison, lang),c);
            System.out.println("Changelog successfully created");
        }catch(Exception e){
            c.setChangeLogSuccessfullyCreated(false);
            System.out.println("Could not generate changelog: "+e.getMessage());
        }
    }
    
    /**
     * Sections of the document. Each section will be a separate html file
     */
    private static void createAbstractSection(String path, Configuration c, Properties languageFile){
        if((c.getAbstractPath()!=null) && (!"".equals(c.getAbstractPath()))){
            WidocoUtils.copyExternalResource(c.getAbstractPath(),new File(path+File.separator+"abstract-"+c.getCurrentLanguage()+".html"));
        }else{
            saveDocument(path+File.separator+"abstract-"+c.getCurrentLanguage()+".html", Constants.getAbstractSection(c.getAbstractSection(),c, languageFile),c);
        }
        
    }
    
    private static void createIntroductionSection(String path, HashMap<String,String> nsDecl, Configuration c, Properties lang){
        if((c.getIntroductionPath()!=null) && (!"".equals(c.getIntroductionPath()))){
            WidocoUtils.copyExternalResource(c.getIntroductionPath(),new File(path+File.separator+"introduction-"+c.getCurrentLanguage()+".html"));
        }else{
            String introSectionText = Constants.getIntroductionSection(c, lang);
            if(nsDecl!=null && !nsDecl.isEmpty()){
                introSectionText += Constants.getNameSpaceDeclaration(nsDecl, lang);
                //small fix: use prefix selected by user.
                if(c.getMainOntology().getNamespacePrefix()!=null && !"".equals(c.getMainOntology().getNamespacePrefix()))
                    introSectionText = introSectionText.replace("default namespace", c.getMainOntology().getNamespacePrefix());
            }
            //introSection += TextConstants.getNamespaceDeclarations(c, lodeInput);
            saveDocument(path+File.separator+"introduction-"+c.getCurrentLanguage()+".html", introSectionText,c);
        }
    }
    
    //the lists passed onto this method are the fixed lists
    private static void createOverviewSection(String path, Configuration c, String classesList, String propList, String dataPropList, String annotationProps, String namedIndividuals, Properties lang){
        if((c.getOverviewPath()!=null) && (!"".equals(c.getOverviewPath()))){
            WidocoUtils.copyExternalResource(c.getOverviewPath(), new File(path+File.separator+"overview-"+c.getCurrentLanguage()+".html"));
        }else{
            String overViewSection = Constants.getOverviewSection(c, lang);
            if(!"".equals(classesList) && classesList!=null){
                overViewSection+=("<h4>"+lang.getProperty(Constants.LANG_CLASSES)+"</h4>\n");
                overViewSection+=(classesList);
            }
            if(!"".equals(propList) && propList!=null){
                overViewSection+=("<h4>"+lang.getProperty(Constants.LANG_OBJ_PROP)+"</h4>");
                overViewSection+=(propList);
            }
            if(!"".equals(dataPropList) && dataPropList!=null){
                overViewSection+=("<h4>"+lang.getProperty(Constants.LANG_DATA_PROP)+"</h4>");
                overViewSection+=(dataPropList);
            }
            if(!"".equals(annotationProps) && annotationProps!=null && c.isIncludeAnnotationProperties()){
                overViewSection+=("<h4>"+lang.getProperty(Constants.LANG_ANN_PROP)+"</h4>");
                overViewSection+=(annotationProps);
            }
            if(!"".equals(namedIndividuals) && namedIndividuals!=null && c.isIncludeNamedIndividuals()){
                overViewSection+=("<h4>"+lang.getProperty(Constants.LANG_NAMED_INDIV)+"</h4>");
                overViewSection+=(namedIndividuals);
            }
            //add the webvowl diagram
            overViewSection +="<iframe align=\"center\" width=\"100%\" height =\"500px\" src=\"webvowl/index.html#ontology\"></iframe> ";
            saveDocument(path+File.separator+"overview-"+c.getCurrentLanguage()+".html", overViewSection,c);
        }
    }
    
    private static void createDescriptionSection(String path, Configuration c, Properties lang){
        if((c.getDescriptionPath()!=null) && (!"".equals(c.getDescriptionPath()))){
            WidocoUtils.copyExternalResource(c.getDescriptionPath(), new File(path+File.separator+"description-"+c.getCurrentLanguage()+".html"));
        }else{
            saveDocument(path+File.separator+"description-"+c.getCurrentLanguage()+".html",Constants.getDescriptionSection(c,lang),c);
        }
    }
    
    private static void createCrossReferenceSection(String path,LODEParser lodeParser, Configuration c, Properties lang){
        //cross reference section has to be included always.
        String crossRef = Constants.getCrossReferenceSection(c, lang);
        String classesList = lodeParser.getClassList(),propList = lodeParser.getPropertyList(), dataPropList = lodeParser.getDataPropList(),
                annotationPropList = lodeParser.getAnnotationPropList(), namedIndividualList = lodeParser.getNamedIndividualList();
        if(classesList!=null && !"".equals(classesList)){
            crossRef += lodeParser.getClasses();
        }
        if(propList!=null && !"".equals(propList)){
            crossRef += lodeParser.getProperties();
        }
        if(dataPropList!=null && !"".equals(dataPropList)){
            crossRef += lodeParser.getDataProp();
        }
        if(c.isIncludeAnnotationProperties() && annotationPropList!=null && !"".equals(annotationPropList)){
            crossRef += lodeParser.getAnnotationProp();
        }
        if(c.isIncludeNamedIndividuals() && namedIndividualList!=null && !"".equals(namedIndividualList)){
            crossRef += lodeParser.getNamedIndividuals();
        }
        //add legend
        crossRef+=Constants.getLegend(lang);
        saveDocument(path+File.separator+"crossref-"+c.getCurrentLanguage()+".html", crossRef,c);
    }
    
    private static void createReferencesSection(String path, Configuration c, Properties lang){
        if((c.getReferencesPath()!=null) && (!"".equals(c.getReferencesPath()))){
            WidocoUtils.copyExternalResource(c.getReferencesPath(), new File(path+File.separator+"overview-"+c.getCurrentLanguage()+".html"));
        }else{
            saveDocument(path+File.separator+"references-"+c.getCurrentLanguage()+".html", Constants.getReferencesSection(c, lang),c);
        }
    }
    
    /**
     * Method for creating the index section on the url provided. The index will
     * include the pointers to all of the other sections.
     */
    private static void createIndexDocument(String path, Configuration c, LODEParser l, Properties lang){
        //the boolean valuas come from the configuration.
        String textToWrite = Constants.getIndexDocument("resources",c, l, lang);
        saveDocument(path+File.separator+"index-"+c.getCurrentLanguage()+".html", textToWrite,c);
    }
    
    //This method should be separated in another utils file.
    public static void saveDocument(String path, String textToWrite, Configuration c){
        File f = new File(path);
        Writer out = null;
        try{
            if(f.exists()){
                //JOptionPane.showMessageDialog(null, "You have overwritten the previous file. This message should be better prepared.");
                if(!c.getOverWriteAll()){
                    String[] options = new String[] {"Rewrite all", "Yes", "No"};
                    int response = JOptionPane.showOptionDialog(null, "The file "+f.getName()+" already exists. Do you want to overwrite it?", "Existing File!",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null, options, options[0]);
                    //0 -> yes to all. 1 -> Yes. 2-> No
                    if(response == 0)c.setOverwriteAll(true); 
                    if(response == 2)return; //else we continue rewriting the file.
                }
            }
            else{
                f.createNewFile();
            }
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
            out.write(textToWrite);
            out.close();
        }catch(IOException e){
            System.err.println("Error while creating the file "+e.getMessage()+"\n"+f.getAbsolutePath());
        }        
        
    }
    
    private static void createFolderStructure(String s, Configuration c, Properties lang){
        File f = new File(s);
        File sections = new File(s+File.separator+"sections");
        File img = new File(s+File.separator+"img");
        File provenance = new File(s+File.separator+"provenance");
        File resources = new File(s+File.separator+"resources");
        File webvowl = new File(s+File.separator+"webvowl");
        if(!f.exists()){
            f.mkdirs();
        }else{
            if(!f.isDirectory()){
                System.err.println("The selected file is not a directory.");
                //throw appropriate exceptions here
            }            
        }
        sections.mkdir();
        if(c.isIncludeDiagram())img.mkdir();
        if(c.isPublishProvenance()){
            provenance.mkdir();
            //do all provenance related stuff here
        }
        resources.mkdir();
        webvowl.mkdir();
        //copy jquery
        WidocoUtils.copyLocalResource("/lode/jquery.js",new File(resources.getAbsolutePath()+File.separator+"jquery.js"));
        WidocoUtils.copyLocalResource("/lode/marked.min.js",new File(resources.getAbsolutePath()+File.separator+"marked.min.js"));
        //copy css
        if(c.isUseW3CStyle()){
            WidocoUtils.copyLocalResource("/lode/lodeprimer.css", new File(resources.getAbsolutePath()+File.separator+"primer.css"));
            WidocoUtils.copyLocalResource("/lode/rec.css", new File(resources.getAbsolutePath()+File.separator+"rec.css"));
            WidocoUtils.copyLocalResource("/lode/extra.css", new File(resources.getAbsolutePath()+File.separator+"extra.css"));
            WidocoUtils.copyLocalResource("/lode/owl.css", new File(resources.getAbsolutePath()+File.separator+"owl.css"));
        }else{
            WidocoUtils.copyLocalResource("/lode/bootstrap-yeti.css", new File(resources.getAbsolutePath()+File.separator+"yeti.css"));
            WidocoUtils.copyLocalResource("/lode/site.css", new File(resources.getAbsolutePath()+File.separator+"site.css"));
        }
        //diagram information
        WidocoUtils.unZipIt(Constants.WEBVOWL_RESOURCES, webvowl.getAbsolutePath());
        //copy widoco readme
        WidocoUtils.copyLocalResource("/widoco/readme.md", new File(f.getAbsolutePath()+File.separator+"readme.md"));
        if(c.isCreateHTACCESS()){
            create406Page(s+File.separator+"406.html",c,lang);
        }
    }

    
    public static void saveConfigFile(String path, Configuration conf)throws IOException{
        String textProperties = "\n";//the first line I leave an intro because there have been problems.
            textProperties+=Constants.ABSTRACT_SECTION_CONTENT+"="+conf.getAbstractSection()+"\n";
            textProperties+=Constants.ONT_TITLE+"="+conf.getMainOntology().getTitle()+"\n";
            textProperties+=Constants.ONT_PREFIX+"="+conf.getMainOntology().getNamespacePrefix()+"\n";
            textProperties+=Constants.ONT_NAMESPACE_URI+"="+conf.getMainOntology().getNamespaceURI()+"\n";
            textProperties+=Constants.ONT_NAME+"="+conf.getMainOntology().getName()+"\n";
            textProperties+=Constants.THIS_VERSION_URI+"="+conf.getMainOntology().getThisVersion()+"\n";
            textProperties+=Constants.LATEST_VERSION_URI+"="+conf.getMainOntology().getLatestVersion()+"\n";
            textProperties+=Constants.PREVIOUS_VERSION+"="+conf.getMainOntology().getPreviousVersion()+"\n";
            textProperties+=Constants.DATE_OF_RELEASE+"="+conf.getMainOntology().getReleaseDate()+"\n";
            textProperties+=Constants.ONTOLOGY_REVISION+"="+conf.getMainOntology().getRevision()+"\n";
            textProperties+=Constants.LICENSE_URI+"="+conf.getMainOntology().getLicense().getUrl()+"\n";
            textProperties+=Constants.LICENSE_NAME+"="+conf.getMainOntology().getLicense().getName()+"\n";
            textProperties+=Constants.LICENSE_ICON_URL+"="+conf.getMainOntology().getLicense().getIcon()+"\n";
            textProperties+=Constants.CITE_AS+"="+conf.getMainOntology().getCiteAs()+"\n";
            textProperties+=Constants.DOI+"="+conf.getMainOntology().getDoi()+"\n";
            textProperties+=Constants.STATUS+"="+conf.getMainOntology().getStatus()+"\n";
            textProperties+=Constants.COMPATIBLE+"="+conf.getMainOntology().getBackwardsCompatibleWith()+"\n";
            if(conf.getMainOntology().getPublisher()!=null){
                textProperties+=Constants.PUBLISHER+"="+conf.getMainOntology().getPublisher().getName()+"\n";
                textProperties+=Constants.PUBLISHER_URI+"="+conf.getMainOntology().getPublisher().getURL()+"\n";
                textProperties+=Constants.PUBLISHER_INSTITUTION+"="+conf.getMainOntology().getPublisher().getInstitutionName()+"\n";
                textProperties+=Constants.PUBLISHER_INSTITUTION_URI+"="+conf.getMainOntology().getPublisher().getInstitutionURL()+"\n";
            }
            String authors="", authorURLs="", authorInstitutions="",authorInstitutionURLs="";
            ArrayList<Agent> ag = conf.getMainOntology().getCreators();
            if(!ag.isEmpty()){
                for(int i=0; i<ag.size()-1;i++){
                    Agent a = ag.get(i);
                    if(a.getName()!=null)authors+=a.getName();
                    authors+=";";
                    if(a.getURL()!=null)authorURLs+=a.getURL();
                    authorURLs+=";";
                    if(a.getInstitutionName()!=null)authorInstitutions+=a.getInstitutionName();
                    authorInstitutions+=";";
                    if(a.getInstitutionURL()!=null)authorInstitutionURLs+=a.getInstitutionURL();
                    authorInstitutionURLs+=";";
                }
                //last agent: no ";"
                if(ag.get(ag.size()-1).getName()!=null) authors+=ag.get(ag.size()-1).getName();
                if(ag.get(ag.size()-1).getURL()!=null) authorURLs+=ag.get(ag.size()-1).getURL();
                if(ag.get(ag.size()-1).getInstitutionName()!=null) authorInstitutions+=ag.get(ag.size()-1).getInstitutionName();
                if(ag.get(ag.size()-1).getInstitutionURL()!=null) authorInstitutionURLs+=ag.get(ag.size()-1).getInstitutionURL();
            }
            textProperties+=Constants.AUTHORS+"="+authors+"\n";
            textProperties+=Constants.AUTHORS_URI+"="+authorURLs+"\n";
            textProperties+=Constants.AUTHORS_INSTITUTION+"="+authorInstitutions+"\n";
            textProperties+=Constants.AUTHORS_INSTITUTION_URI+"="+authorInstitutionURLs+"\n";
            
            ag = conf.getMainOntology().getContributors();
            authors=""; 
            authorURLs="";
            authorInstitutions="";
            authorInstitutionURLs="";
            if(!ag.isEmpty()){
                for(int i=0; i<ag.size()-1;i++){
                    Agent a = ag.get(i);
                    if(a.getName()!=null)authors+=a.getName();
                    authors+=";";
                    if(a.getURL()!=null)authorURLs+=a.getURL();
                    authorURLs+=";";
                    if(a.getInstitutionName()!=null)authorInstitutions+=a.getInstitutionName();
                    authorInstitutions+=";";
                    if(a.getInstitutionURL()!=null)authorInstitutionURLs+=a.getInstitutionURL();
                    authorInstitutionURLs+=";";
                }
                if(ag.get(ag.size()-1).getName()!=null) authors+=ag.get(ag.size()-1).getName();
                if(ag.get(ag.size()-1).getURL()!=null) authorURLs+=ag.get(ag.size()-1).getURL();
                if(ag.get(ag.size()-1).getInstitutionName()!=null) authorInstitutions+=ag.get(ag.size()-1).getInstitutionName();
                if(ag.get(ag.size()-1).getInstitutionURL()!=null) authorInstitutionURLs+=ag.get(ag.size()-1).getInstitutionURL();
            }
            textProperties+=Constants.CONTRIBUTORS+"="+authors+"\n";
            textProperties+=Constants.CONTRIBUTORS_URI+"="+authorURLs+"\n";
            textProperties+=Constants.CONTRIBUTORS_INSTITUTION+"="+authorInstitutions+"\n";
            textProperties+=Constants.CONTRIBUTORS_INSTITUTION_URI+"="+authorInstitutionURLs+"\n";
            String importedNames="", importedURIs="";
            ArrayList<Ontology> imported = conf.getMainOntology().getImportedOntologies();
            if(!imported.isEmpty()){
                for(int i=0; i<imported.size()-1;i++){
                    Ontology o = imported.get(i);
                    if(o.getName()!=null)importedNames+=o.getName();
                    importedNames+=";";
                    if(o.getNamespaceURI()!=null)importedURIs+=o.getNamespaceURI();
                    importedURIs+=";";
                }
                //last agent: no ";"
                if(imported.get(imported.size()-1).getName()!=null) importedNames+=imported.get(imported.size()-1).getName();
                if(imported.get(imported.size()-1).getNamespaceURI()!=null) importedURIs+=imported.get(imported.size()-1).getNamespaceURI();
            }
            textProperties+=Constants.IMPORTED_ONTOLOGY_NAMES+"="+importedNames+"\n";
            textProperties+=Constants.IMPORTED_ONTOLOGY_URIS+"="+importedURIs+"\n";
            imported = conf.getMainOntology().getExtendedOntologies();
            importedNames = "";
            importedURIs = "";
            if(!imported.isEmpty()){
                for(int i=0; i<imported.size()-1;i++){
                    Ontology o = imported.get(i);
                    if(o.getName()!=null)importedNames+=o.getName();
                    importedNames+=";";
                    if(o.getNamespaceURI()!=null)importedURIs+=o.getNamespaceURI();
                    importedURIs+=";";
                }
                //last agent: no ";"
                if(imported.get(imported.size()-1).getName()!=null) importedNames+=imported.get(imported.size()-1).getName();
                if(imported.get(imported.size()-1).getNamespaceURI()!=null) importedURIs+=imported.get(imported.size()-1).getNamespaceURI();
            }
            textProperties+=Constants.EXTENDED_ONTOLOGY_NAMES+"="+importedNames+"\n";
            textProperties+=Constants.EXTENDED_ONTOLOGY_URIS+"="+importedURIs+"\n";
            //serializations
            HashMap<String,String> serializations = conf.getMainOntology().getSerializations();
            if(serializations.containsKey("RDF/XML")){
                textProperties+=Constants.RDF+"="+serializations.get("RDF/XML")+"\n";
            }
            if(serializations.containsKey("TTL")){
                textProperties+=Constants.TTL+"="+serializations.get("TTL")+"\n";
            }
            if(serializations.containsKey("N-Triples")){
                textProperties+=Constants.N3+"="+serializations.get("N-Triples")+"\n";
            }
            if(serializations.containsKey("JSON-LD")){
                textProperties+=Constants.JSON+"="+serializations.get("JSON-LD")+"\n";
            }
            //copy the result into the file
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "utf-8"));
                writer.write(textProperties);
                //JOptionPane.showMessageDialog(this, "Property file saved successfully");
            } catch (IOException ex) {
              System.err.println("Error while saving the property file "+ex.getMessage());
              throw ex;
            } finally {
               try {
                   if(writer!=null)writer.close();
               } catch (IOException ex) {}
            }
    }
    
}
