#include <delay.h>
#include <gpio.h>
#include <stm32.h>
#include <string.h>


// LEDS //
    // GPIO's
    #define RED_LED_GPIO GPIOA
    #define GREEN_LED_GPIO GPIOA
    #define BLUE_LED_GPIO GPIOB
    // PINS
    #define RED_LED_PIN 6
    #define GREEN_LED_PIN 7
    #define BLUE_LED_PIN 0
    // functions
    #define RedLEDon() RED_LED_GPIO->BSRRH = 1 << RED_LED_PIN
    #define RedLEDoff() RED_LED_GPIO->BSRRL = 1 << RED_LED_PIN
    #define GreenLEDon() GREEN_LED_GPIO->BSRRH = 1 << GREEN_LED_PIN
    #define GreenLEDoff() GREEN_LED_GPIO->BSRRL = 1 << GREEN_LED_PIN
    #define BlueLEDon() BLUE_LED_GPIO->BSRRH = 1 << BLUE_LED_PIN
    #define BlueLEDoff() BLUE_LED_GPIO->BSRRL = 1 << BLUE_LED_PIN
//

// JOYSTICK
    // GPIO
    #define JOYSTICK_GPIO GPIOB
    // PINS
    #define BUTTON_FIRE_PIN 10
    #define BUTTON_LEFT_PIN 3
    #define BUTTON_RIGHT_PIN 4
    #define BUTTON_UP_PIN 5
    #define BUTTON_DOWN_PIN 6
    // functions
    #define IsButtonFIREOn() (!(JOYSTICK_GPIO->IDR & (1 << BUTTON_FIRE_PIN)))
    #define IsButtonUPOn() (!(JOYSTICK_GPIO->IDR & (1 << BUTTON_UP_PIN)))
    #define IsButtonDOWNOn() (!(JOYSTICK_GPIO->IDR & (1 << BUTTON_DOWN_PIN)))
    #define IsButtonLEFTOn() (!(JOYSTICK_GPIO->IDR & (1 << BUTTON_LEFT_PIN)))
    #define IsButtonRIGHTOn() (!(JOYSTICK_GPIO->IDR & (1 << BUTTON_RIGHT_PIN)))
//

// USART1 //
    // GPIO
    #define USART_GPIO GPIOA
    //PINS
    #define USART_TXD_PIN 9
    #define USART_RXD_PIN 10
    //CR1
    #define USART_Mode_Rx_Tx (USART_CR1_RE | USART_CR1_TE)
    #define USART_Enable USART_CR1_UE
    #define USART_WordLength_8b 0x0000
    #define USART_Parity_No 0x0000
    //CR2
    #define USART_StopBits_1 0x0000
    //CR3
    #define USART_FlowControl_None 0x0000
    // USART_BRR
    #define HSI_HZ 16000000U
    #define PCLK2_HZ HSI_HZ
    // functions
    #define IsCharOnInput() (USART1->SR & USART_SR_RXNE)
    #define CanCharBeSent() (USART1->SR & USART_SR_TXE)
// USART1 //

// COMMANDS
    #define JB_OFF 0
    #define JB_ON 1

    #define	FIRE_PRESSED 'F'
    #define	FIRE_RELEASED 'f'

    #define	UP_PRESSED 'U'
    #define	UP_RELEASED 'u'

    #define	RIGHT_PRESSED 'R'
    #define	RIGHT_RELEASED 'r'

    #define	DOWN_PRESSED 'D'
    #define	DOWN_RELEASED 'd'

    #define	LEFT_PRESSED 'L'
    #define	LEFT_RELEASED 'l'

    #define RED_ON 'R'
    #define RED_OFF 'r'

    #define GREEN_ON 'G'
    #define GREEN_OFF 'g'

    #define BLUE_ON 'B'
    #define BLUE_OFF 'b'

//

#define BUFF_SIZE 128
char snd_buffer[BUFF_SIZE];
int snd_buffer_pos = 0, snd_buffer_buffered = 0;

int last_state_button_U, last_state_button_D, last_state_button_R, last_state_button_L, last_state_button_F;


void rcc_config() {
    RCC->AHB1ENR |= RCC_AHB1ENR_GPIOAEN | RCC_AHB1ENR_GPIOBEN;
    RCC->APB2ENR |= RCC_APB2ENR_SYSCFGEN; // PRZERWANIA
    RCC->APB2ENR |= RCC_APB2ENR_USART1EN; // USART1 - bt
//    __NOP();
}

void configure_leds() {
    RedLEDoff();
    GreenLEDoff();
    BlueLEDoff();

    GPIOoutConfigure(RED_LED_GPIO,
                     RED_LED_PIN,
                     GPIO_OType_PP,
                     GPIO_Low_Speed,
                     GPIO_PuPd_NOPULL);

    GPIOoutConfigure(BLUE_LED_GPIO,
                     BLUE_LED_PIN,
                     GPIO_OType_PP,
                     GPIO_Low_Speed,
                     GPIO_PuPd_NOPULL);

    GPIOoutConfigure(GREEN_LED_GPIO,
                     GREEN_LED_PIN,
                     GPIO_OType_PP,
                     GPIO_Low_Speed,
                     GPIO_PuPd_NOPULL);
}



void add_to_buff(char a) {
    snd_buffer[snd_buffer_buffered] = a;
    snd_buffer_buffered++;
    snd_buffer_buffered %= BUFF_SIZE;
}

void bt_config() {
    GPIOafConfigure(USART_GPIO, USART_TXD_PIN, GPIO_OType_PP, GPIO_Fast_Speed,
                    GPIO_PuPd_NOPULL, GPIO_AF_USART1);

    GPIOafConfigure(USART_GPIO, USART_RXD_PIN, GPIO_OType_PP, GPIO_Fast_Speed,
                    GPIO_PuPd_UP, GPIO_AF_USART1);

    USART1->CR1 = USART_Mode_Rx_Tx | USART_WordLength_8b | USART_Parity_No;
    USART1->CR2 = USART_StopBits_1;
    USART1->CR3 = USART_FlowControl_None;
    uint32_t const baudrate = 9600U;
    USART1->BRR = (PCLK2_HZ/4U) / baudrate; // predkość magistrali przez baud
}

int try_to_read_char(char *single_char) {
    if (IsCharOnInput()) {
        *single_char = USART1->DR;
        return 1;
    } else {
        return 0;
    }
}

int try_to_send_char(char data) {
    if (CanCharBeSent()) {
        USART1->DR = data;
        return 1;
    } else {
        return 0;
    }
}

void EXTI15_10_IRQHandler(void) {
    if (EXTI->PR & EXTI_PR_PR10) {
        EXTI->PR = EXTI_PR_PR10;
        if (IsButtonFIREOn() ^ last_state_button_F) {
            if (last_state_button_F == JB_ON) {
                add_to_buff(FIRE_RELEASED);
                last_state_button_F = JB_OFF;
            } else {
                add_to_buff(FIRE_PRESSED);
                last_state_button_F = JB_ON;
            }
        }
    }
}

void EXTI9_5_IRQHandler(void) {
    if (EXTI->PR & EXTI_PR_PR6) {
        EXTI->PR = EXTI_PR_PR6;
        if (IsButtonDOWNOn()^last_state_button_D) {
            if (last_state_button_D == JB_ON) {
                add_to_buff(DOWN_RELEASED);
                last_state_button_D = JB_OFF;
            } else {
                add_to_buff(DOWN_PRESSED);
                last_state_button_D = JB_ON;
            }
        }
    }

    if (EXTI->PR & EXTI_PR_PR5) {
        EXTI->PR = EXTI_PR_PR5;
        if (IsButtonUPOn()^last_state_button_U) {
            if (last_state_button_U == JB_ON) {
                add_to_buff(UP_RELEASED);
                last_state_button_U = JB_OFF;
            } else {
                add_to_buff(UP_PRESSED);
                last_state_button_U = JB_ON;
            }
        }
    }
}

void EXTI4_IRQHandler(void) {
    if (EXTI->PR & EXTI_PR_PR4) {
        EXTI->PR = EXTI_PR_PR4;
        if (IsButtonRIGHTOn()^last_state_button_R) {
            if (last_state_button_R == JB_ON) {
                add_to_buff(RIGHT_RELEASED);
                last_state_button_R = JB_OFF;
            } else {
                add_to_buff(RIGHT_PRESSED);
                last_state_button_R = JB_ON;
            }
        }
    }
}

void EXTI3_IRQHandler(void) {
    if (EXTI->PR & EXTI_PR_PR3) {
        EXTI->PR = EXTI_PR_PR3;
        if (IsButtonLEFTOn()^last_state_button_L) {
            if (last_state_button_L == JB_ON) {
                add_to_buff(LEFT_RELEASED);
                last_state_button_L = JB_OFF;
            } else {
                add_to_buff(LEFT_PRESSED);
                last_state_button_L = JB_ON;
            }
        }
    }
}


void configure_buttons() {
    last_state_button_R = IsButtonRIGHTOn();
    last_state_button_L = IsButtonLEFTOn();
    last_state_button_U = IsButtonUPOn();
    last_state_button_D = IsButtonDOWNOn();
    last_state_button_F = IsButtonFIREOn();

    GPIOinConfigure(JOYSTICK_GPIO, BUTTON_FIRE_PIN, GPIO_PuPd_UP, EXTI_Mode_Interrupt, EXTI_Trigger_Rising_Falling);
    GPIOinConfigure(JOYSTICK_GPIO, BUTTON_LEFT_PIN, GPIO_PuPd_UP, EXTI_Mode_Interrupt, EXTI_Trigger_Rising_Falling);
    GPIOinConfigure(JOYSTICK_GPIO, BUTTON_RIGHT_PIN, GPIO_PuPd_UP, EXTI_Mode_Interrupt, EXTI_Trigger_Rising_Falling);
    GPIOinConfigure(JOYSTICK_GPIO, BUTTON_UP_PIN, GPIO_PuPd_UP, EXTI_Mode_Interrupt, EXTI_Trigger_Rising_Falling);
    GPIOinConfigure(JOYSTICK_GPIO, BUTTON_DOWN_PIN, GPIO_PuPd_UP, EXTI_Mode_Interrupt, EXTI_Trigger_Rising_Falling);

    NVIC_EnableIRQ(EXTI15_10_IRQn);
    NVIC_EnableIRQ(EXTI9_5_IRQn);
    NVIC_EnableIRQ(EXTI4_IRQn);
    NVIC_EnableIRQ(EXTI3_IRQn);
    NVIC_EnableIRQ(EXTI0_IRQn);
}


void try_to_send_char_from_buffer() {
    if (snd_buffer_pos != snd_buffer_buffered && try_to_send_char(snd_buffer[snd_buffer_pos])) {
        snd_buffer_pos++;
        snd_buffer_pos %= BUFF_SIZE;
    }
}

void execute_bt_command_if_any() {
    char i;
    if (try_to_read_char(&i)) {
        switch (i) {
            case RED_ON: RedLEDon();
                break;
            case RED_OFF: RedLEDoff();
                break;
            case GREEN_ON: GreenLEDon();
                break;
            case GREEN_OFF: GreenLEDoff();
                break;
            case BLUE_ON: BlueLEDon();
                break;
            case BLUE_OFF: BlueLEDoff();
                break;
        }
    }
}

int main() {
    rcc_config();
    configure_leds();
    bt_config();
    configure_buttons();
    USART1->CR1 |= USART_Enable;
    Delay(1000);
    while(1) {
        try_to_send_char_from_buffer();
        execute_bt_command_if_any();
        __NOP();
//        __NOP();
//        __NOP();
//        try_to_send_char('a');
//        Delay(1000);
    }
}