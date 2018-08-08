package models;

import com.google.inject.ImplementedBy;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * This interface provides a non-blocking API for possibly blocking operations.
 */
@ImplementedBy(JPAAd.class)
public interface AdRepository {


    CompletionStage<List<Advertisement>> list();
    CompletionStage<List<Advertisement>> update(List<Advertisement> ads);

}