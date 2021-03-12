package com.ipiecoles.batch.csvImport;

//import com.ipiecoles.batch.dto.CommuneCSV;
import com.ipiecoles.batch.dto.CommuneCSV;
import com.ipiecoles.batch.exception.CommuneCSVException;
import com.ipiecoles.batch.exception.NetworkException;
import com.ipiecoles.batch.model.Commune;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.IncorrectTokenCountException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.retry.backoff.FixedBackOffPolicy;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
@PropertySource("classpath:application.properties")
public class CommunesImportBatch /*implements Tasklet*/ {

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    JobBuilderFactory jobBuilderFactory;

    @Autowired
    public EntityManagerFactory entityManagerFactory;

    @Value("${importFile.chunkSize}")
    private Integer CHUNK_SIZE;

    @Value("${importFile.nomJob]")
    private String NOM_JOB;


//    @Autowired
//    private EntityManagerFactory entityManagerFactory;

    @Bean
    public ItemReadListener<CommuneCSV> communeCSVItemReadListener(){
        return new CommuneCSVItemListener();
    }
    @Bean
    public ItemWriteListener<Commune> communeCSVItemWriteListener(){
        return new CommuneCSVItemListener();
    }


    @Bean
    public StepExecutionListener communeCSVImportStepListener(){
        return new CommuneCSVImportStepListener();
    }

    @Bean
    public ChunkListener communeCSVImportChunkListener(){
        return new CommuneCSVImportChunkListener();
    }

    @Bean
    public CommuneCSVItemListener communeCSVItemListener(){
        return new CommuneCSVItemListener();
    }
    @Bean
    public CommunesMissingCoordinatesSkipListener communesMissingCoordinatesSkipListener(){
        return new CommunesMissingCoordinatesSkipListener();
    }

    @Bean
    public CommunesCSVImportSkipListener communesCSVImportSkipListener(){
        return new CommunesCSVImportSkipListener();
    }


    // ex 2e step
    // reader
    @Bean
    public JpaPagingItemReader<Commune> communesMissingCoordinatesJpaItemReader(){
        return new JpaPagingItemReaderBuilder<Commune>()
                .name("communesMissingCoordinatesJpaItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(10)
                .queryString("from Commune c where c.latitude is null or c.longitude is null")
                .build();
    }

    //processor
    @Bean
    public CommunesMissingCoordinatesItemProcessor communesMissingCoordinatesItemProcessor(){
        return new CommunesMissingCoordinatesItemProcessor();
    }

    // writer : on récupère le writer jpa existant

    // step
//    @Bean
//    public Step stepGetMissingCoordinates(){
//        return stepBuilderFactory.get("getMissingCoordinates")
//                .<Commune, Commune> chunk(10)
//                .reader(communesMissingCoordinatesJpaItemReader())
//                .processor(communesMissingCoordinatesItemProcessor())
//                .writer(writerJPA())
//                .build();
//    }
//    public Step stepGetMissingCoordinates(){ // à modifier pour réessayer
//        return stepBuilderFactory.get("getMissingCoordinates")
//                .<Commune, Commune> chunk(10)
//                .reader(communesMissingCoordinatesJpaItemReader())
//                .processor(communesMissingCoordinatesItemProcessor())
//                .writer(writerJPA())
////                .faultTolerant()
////                .skipLimit(10)
////                .skip(CommuneCSVException.class)
//                .build();
//    }

    @Bean
    public Step stepGetMissingCoordinates(){
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        policy.setBackOffPeriod(2000);
        return stepBuilderFactory.get("getMissingCoordinates")
                .<Commune, Commune> chunk(10)
                .reader(communesMissingCoordinatesJpaItemReader())
                .processor(communesMissingCoordinatesItemProcessor())
                .writer(writerJPA2())
                .faultTolerant()
                .retryLimit(5)
                .retry(NetworkException.class)
                .backOffPolicy(policy)
                .build();
    }




    // maintenant, on n'a plus qu'à modifier le job
    // sans flot
//    @Bean
//    public Job importCsvJob(Step stepHelloWorld, Step stepImportCSV, Step stepGetMissingCoordinates){
//        return jobBuilderFactory.get("importCsvJob")
//                .incrementer(new RunIdIncrementer())
//                .flow(stepHelloWorld)
//                .next(stepImportCSV)
//                .next(stepGetMissingCoordinates)
//                .end().build();
//    }

    // avec flot
    @Bean
    public Job importCsvJob(Step stepHelloWorld, Step stepImportCSV, Step stepGetMissingCoordinates){
        return jobBuilderFactory.get("importCsvJob")
                .incrementer(new RunIdIncrementer())
                .flow(stepHelloWorld)
                .next(stepImportCSV)
                .on("COMPLETED_WITH_MISSING_COORDINATES").to(stepGetMissingCoordinates)
                .end().build();
    }


    @Bean
    public JpaItemWriter<Commune> writerJPA2() {
        return new JpaItemWriterBuilder<Commune>().entityManagerFactory(entityManagerFactory).build();
    }





//    @Bean
//    public JpaItemWriter<Commune> writerJPA(){
//        return new JpaItemWriterBuilder<Commune>().entityManagerFactory(entityManagerFactory)
//                .build();
//    }

    @Bean
    public JpaItemWriter<Commune> writerJPA() {
        return new JpaItemWriterBuilder<Commune>().entityManagerFactory(entityManagerFactory).build();
    }

    // entraîne erreur de point d'interrogation
//    @Bean
//    public JdbcBatchItemWriter<Commune> writerJDBC(DataSource dataSource) {
//        return new JdbcBatchItemWriterBuilder<Commune>()
//                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
//                .sql("INSERT INTO Commune (codeInsee, nom, codePostal, libelleAcheminement, coordonneesGPS) "
//                        + "VALUES (':1006', ':Ambleon', ':1300', ':', ':45.7494989044,5.59432017366'")
//                .dataSource(dataSource)
//                .build();
//    }

//    @Bean
//    public JdbcBatchItemWriter<Commune> writerJDBC(DataSource dataSource){
//        return new JdbcBatchItemWriterBuilder<Commune>()
//                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
//                .sql("INSERT INTO COMMUNE(code_insee, nom, code_postal, latitude, longitude) VALUES " +
//                        "(:codeInsee, :nom, :codePostal, :latitude, :longitude) AS c " +
//                        "ON DUPLICATE KEY UPDATE nom=c.nom, code_postal=c.code_postal, latitude=c.latitude, longitude=c.longitude")
//                .dataSource(dataSource).build();
//    }

//    @Bean
//    public ItemWriter<Commune> fileWriter() {
//        BeanWrapperFieldExtractor<Commune> bwfe = new BeanWrapperFieldExtractor<Commune>();
//        bwfe.setNames(new String[] {":1006", ":Ambleon", ":1300", ":", ":45.7494989044,5.59432017366"});
//
//        DelimitedLineAggregator<Commune> agg = new DelimitedLineAggregator<>();
//        agg.setFieldExtractor(bwfe);
//
//        return new FlatFileItemWriterBuilder<>()
//                .name("csvWriter")
//                .lineAggregator(agg)
//                .resource(new FileSystemResource("C:\\dev\\git\\batch\\test.csv"))
//                .delimited().delimiter(";")
//                .build();
//    }

    @Bean
    public CommuneCSVItemProcessor communeCSVToCommuneProcessor() {
        return new CommuneCSVItemProcessor();
    }


    @Bean
    public FlatFileItemReader<CommuneCSV> communesCSVItemReader() {
        return new FlatFileItemReaderBuilder<CommuneCSV>()
                .name("communesCSVItemReader")
                .linesToSkip(1)
//                .resource(new ClassPathResource("laposte_hexasmal_test.csv"))
                .resource(new ClassPathResource("laposte_hexasmal.csv")) //laposte_hexasmal_test_skip
                .delimited()
                .delimiter(";")
                .names("codeInsee", "nom", "codePostal", "ligne5", "libelleAcheminement", "coordonneesGPS")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {
                    {
                        setTargetType(CommuneCSV.class);
                    }
                })
                .build();
    }

    @Bean
//    public Step stepHelloWorld(Tasklet stepCommune) {
    public Step stepHelloWorld() {
        return stepBuilderFactory.get("stepHelloWorld")
//                .tasklet(helloWorldTasklet())
                .tasklet(helloWorldTasklet())
                .listener(helloWorldTasklet())
                .build();
    }

    @Bean
    public Tasklet helloWorldTasklet() {
        return new HelloWorldTasklet();
    }

//    @Bean
//    public Job importCsvJob(Step stepHelloWorld) {
//    public Job importCsvJob(Step importCsvStep) {
//        return jobBuilderFactory.get("importCsvJob")
//                .incrementer(new RunIdIncrementer())
//                .flow(importCsvStep)
//                .end()
//                .build();
//    }


//    @Bean
//    public Job importCsvJob(Step stepHelloWorld, Step stepImportCSV){
//        return jobBuilderFactory.get("importFile")
//                .incrementer(new RunIdIncrementer())
//                .flow(stepHelloWorld)
//                .next(stepImportCSV)
//                .end().build();
//    }

//    @Bean
//    public Job importCsvJob(Step stepImportCSV, Step stepAddCoord){
////        return jobBuilderFactory.get("importFile")
//        return jobBuilderFactory.get("importCsvJob")
//                .incrementer(new RunIdIncrementer())
//                .flow(stepImportCSV)
//                .next(stepAddCoord)
//                .end().build();
//    }





//    @Bean
    // la gestion de dépendance, on peut la faire de deux manières : soit on la met en paramètre, soit on la met directement dans le get, on met alors "null" comme valeur
//    public Step importCsvStep(FlatFileItemReader<CommuneCSV> communesCSVItemReader, JpaItemWriter<Commune> writer) {
//        return stepBuilderFactory.get("importFileStep")
//                .<CommuneCSV, Commune>chunk(CHUNK_SIZE)
//                .reader(communesCSVItemReader())
//                .processor(communeCSVToCommuneProcessor())
//                .writer(writerJPA())
//                .build();
//    }


//    @Bean
//    public Step stepImportCSV(){
////        return stepBuilderFactory.get("importFile")
//        return stepBuilderFactory.get("stepImportCSV")
//                .<CommuneCSV, Commune> chunk(10)
//                .reader(communesCSVItemReader())
//                .processor(communeCSVToCommuneProcessor())
//                .writer(writerJPA())
//                .build();
//    }

    // sans listener
//    @Bean
//    public Step stepImportCSV(){
//        return stepBuilderFactory.get("importFile")
//                .<CommuneCSV, Commune> chunk(CHUNK_SIZE)
//                .reader(communesCSVItemReader())
//                .processor(communeCSVToCommuneProcessor())
//                .writer(writerJPA())
//                .faultTolerant()
//                .skipLimit(100)
//                .skip(CommuneCSVException.class)
////                .skip(IncorrectTokenCountException.class) // exception copié dan les logs
//                .skip(FlatFileParseException.class) // exception copié dan les logs
//                .build();
//    }

    // avec lister
//    @Bean
//    public Step stepImportCSV(){
//        return stepBuilderFactory.get("importFile")
//                .<CommuneCSV, Commune> chunk(CHUNK_SIZE)
//                .reader(communesCSVItemReader())
//                .processor(communeCSVToCommuneProcessor())
//                .writer(writerJPA())
//                .faultTolerant()
//                .skipLimit(100)
//                .skip(CommuneCSVException.class)
//                .skip(FlatFileParseException.class) // exception copié dan les logs
//                .listener(communesMissingCoordinatesSkipListener())
//                .listener(communeCSVImportStepListener())
//                .listener(communeCSVImportChunkListener())
//                .listener(communeCSVItemListener())
//                .listener(communeCSVToCommuneProcessor())
//                .build();
//    }
    // sans flot
//    @Bean
//    public Step stepImportCSV(){
//        return stepBuilderFactory.get("importFile")
//                .<CommuneCSV, Commune> chunk(CHUNK_SIZE)
//                .reader(communesCSVItemReader())
//                .processor(communeCSVToCommuneProcessor())
//                .writer(writerJPA())
//                .faultTolerant()
//                .skipPolicy(new AlwaysSkipItemSkipPolicy())
//                .skip(CommuneCSVException.class)
//                .skip(FlatFileParseException.class)
//                .listener(communesCSVImportSkipListener())
//                .listener(communeCSVImportStepListener())
//                .listener(communeCSVImportChunkListener())
////                .listener(communeCSVItemListener())
//                .listener(communeCSVItemReadListener())
//                .listener(communeCSVItemWriteListener())
//                .listener(communeCSVToCommuneProcessor())
//                .build();
//    }

    // avec flot
    @Bean
    public Step stepImportCSV(){
        return stepBuilderFactory.get("importFile")
                .<CommuneCSV, Commune> chunk(CHUNK_SIZE)
                .reader(communesCSVItemReader())
                .processor(communeCSVToCommuneProcessor())
                .writer(writerJPA())
                .faultTolerant()
                .skipPolicy(new AlwaysSkipItemSkipPolicy())
                .skip(CommuneCSVException.class)
                .skip(FlatFileParseException.class)
                .listener(communesCSVImportSkipListener())
//                .listener(communeCSVImportStepListener())
//                .listener(communeCSVImportChunkListener())
//                .listener(communeCSVItemReadListener())
                .listener(communeCSVItemWriteListener())
                .listener(communeCSVToCommuneProcessor())
                .build();
    }

//    @Bean
//    public Step stepAddCoord(){
//        return stepBuilderFactory.get("stepAddCoord")
//                .<CommuneCSV, Commune> chunk(10)
//                .reader(communesCSVItemReader())
//                .processor(communeCSVToCommuneProcessor())
//                .writer(writerJPA())
//                .build();
//    }

//    @Override
//    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
//        return null;
//    }

}
