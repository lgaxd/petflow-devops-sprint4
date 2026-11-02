package br.com.petflow.petflow_api;

import br.com.petflow.petflow_api.entity.RewardAction;
import br.com.petflow.petflow_api.repository.RewardActionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardActionInitializerTest {

    @Mock
    private RewardActionRepository rewardActionRepository;

    @InjectMocks
    private RewardActionInitializer initializer;

    @Test
    void shouldCreateMissingRewardActionsFromOracleReference() {
        when(rewardActionRepository.findByNameIgnoreCase("CADASTRO_PET")).thenReturn(Optional.empty());
        when(rewardActionRepository.findByNameIgnoreCase("EVENTO_SAUDE_REALIZADO")).thenReturn(Optional.empty());
        when(rewardActionRepository.findByNameIgnoreCase("ASSINATURA_ATIVA")).thenReturn(Optional.empty());
        when(rewardActionRepository.findByNameIgnoreCase("RESGATE_CUPOM")).thenReturn(Optional.empty());

        initializer.run(new String[0]);

        ArgumentCaptor<RewardAction> captor = ArgumentCaptor.forClass(RewardAction.class);
        verify(rewardActionRepository, times(4)).save(captor.capture());

        assertEquals(4, captor.getAllValues().size());
        assertEquals("CADASTRO_PET", captor.getAllValues().get(0).getName());
        assertEquals("EVENTO_SAUDE_REALIZADO", captor.getAllValues().get(1).getName());
        assertEquals("ASSINATURA_ATIVA", captor.getAllValues().get(2).getName());
        assertEquals("RESGATE_CUPOM", captor.getAllValues().get(3).getName());
    }
}
