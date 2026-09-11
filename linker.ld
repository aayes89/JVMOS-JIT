ENTRY(_start)

SECTIONS
{
    /* GRUB carga el kernel a partir de 1MB en la RAM */
    . = 1M;

    .text BLOCK(4K) : ALIGN(4K)
    {
        *(.multiboot)   /* Garantiza que el header Multiboot vaya de primero */
        *(.text*)
    }

    .rodata BLOCK(4K) : ALIGN(4K)
    {
        *(.rodata*)
    }

    .data BLOCK(4K) : ALIGN(16)
    {
        *(.data*)
    }

    /* Pila x86 de arranque protegida */
    .bootstrap_stack BLOCK(4K) : ALIGN(16)
    {
        *(.bootstrap_stack)
    }

     .bss BLOCK(4K) : ALIGN(16)
    {
        bss_start = .;    
        *(COMMON)
        *(.bss*)
        bss_end = .;      
    }
}
