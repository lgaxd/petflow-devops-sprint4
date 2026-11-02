package br.com.petflow.petflow_api;

import br.com.petflow.petflow_api.entity.RewardAction;
import br.com.petflow.petflow_api.repository.RewardActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RewardActionInitializer implements CommandLineRunner {

    private final RewardActionRepository rewardActionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<Seed> requiredSeeds = List.of(
                new Seed("CADASTRO_PET", 5, "Pontos por cadastrar um novo pet"),
                new Seed("EVENTO_SAUDE_REALIZADO", 10, "Pontos por evento de saúde concluído"),
                new Seed("ASSINATURA_ATIVA", 15, "Pontos por assinatura de plano ativa"),
                new Seed("RESGATE_CUPOM", 0, "Consumo de pontos para resgate de cupom")
        );

        for (Seed seed : requiredSeeds) {
            rewardActionRepository.findByNameIgnoreCase(seed.name())
                    .orElseGet(() -> rewardActionRepository.save(
                            RewardAction.builder()
                                    .name(seed.name())
                                    .pointsValue(seed.pointsValue())
                                    .description(seed.description())
                                    .build()
                    ));
        }
    }

    private record Seed(String name, Integer pointsValue, String description) {}
}
