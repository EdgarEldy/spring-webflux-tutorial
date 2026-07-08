package edgareldy.springwebfluxtutorial;

import org.springframework.boot.SpringApplication;

public class TestSpringWebfluxTutorialApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpringWebfluxTutorialApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
