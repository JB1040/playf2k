package models;

import com.google.inject.ImplementedBy;

import play.data.validation.Constraints.Required;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * This interface provides a non-blocking API for possibly blocking operations.
 */
@ImplementedBy(JPAUser.class)
public interface UserRepository {

    CompletionStage<User> add(User us);

    CompletionStage<User> edit(User u);
    
    CompletionStage<Stream<User>> list(int offset,int amount, Boolean isOnline);

	CompletionStage<User> get(long id);

	CompletionStage<User> getByName(String username);
}