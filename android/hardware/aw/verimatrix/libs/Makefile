#-----------------------------------------------------------------------------#
#  Top level make for VMClient                          		      #
#-----------------------------------------------------------------------------#

all: libs

libs:
	@for i in *; \
		do \
			if [ -d "$$i" ]; then \
				echo "$$i"; \
				$(MAKE) --no-print-directory -C $$i; \
			fi; \
		done

clean::
	@for i in *; \
		do \
			if [ -d "$$i" ]; then \
				echo "$$i"; \
				$(MAKE) --no-print-directory -C $$i clean; \
			fi; \
		done

objclean::
	@for i in *; \
		do \
			if [ -d "$$i" ]; then \
				rm -rf $$i/obj; \
			fi; \
		done

